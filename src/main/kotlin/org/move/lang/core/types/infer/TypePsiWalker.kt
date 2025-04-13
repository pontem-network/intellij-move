package org.move.lang.core.types.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.cli.settings.debugErrorOrFallback
import org.move.cli.settings.isDebugModeEnabled
import org.move.cli.settings.moveSettings
import org.move.ide.formatter.impl.location
import org.move.lang.core.completion.providers.dropInvisibleEntries
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.ext.namedFields
import org.move.lang.core.resolve.getEntriesFromWalkingScopes
import org.move.lang.core.resolve.getFieldLookupResolveVariants
import org.move.lang.core.resolve.getMethodResolveVariants
import org.move.lang.core.resolve.ref.NAMES
import org.move.lang.core.resolve.ref.resolveAliases
import org.move.lang.core.resolve.ref.resolvePath
import org.move.lang.core.resolve.scopeEntry.asEntries
import org.move.lang.core.resolve.scopeEntry.filterByName
import org.move.lang.core.resolve.scopeEntry.namedElements
import org.move.lang.core.resolve.scopeEntry.singleItemOrNull
import org.move.lang.core.resolve.scopeEntry.toPathResolveResults
import org.move.lang.core.types.infer.Expectation.NoExpectation
import org.move.lang.core.types.ty.*
import org.move.lang.core.types.ty.TyReference.Companion.autoborrow
import org.move.stdext.RsResult
import org.move.stdext.chain

class TypePsiWalker(
    val ctx: InferenceContext,
    val project: Project,
    private val expectedReturnTy: Ty
) {
    val msl: Boolean get() = ctx.msl

    fun <T> mslScope(action: () -> T): T {
        if (ctx.msl) return action()

        ctx.msl = true
        val res = ctx.freezeUnification { action() }
        ctx.msl = false

        return res
    }

    fun collectParameterBindings(owner: MvInferenceContextOwner) {
        val bindings = when (owner) {
            is MvFunctionLike -> owner.parametersAsBindings
            is MvItemSpec -> {
                val specItem = owner.item
                when (specItem) {
                    is MvFunction -> {
                        specItem.parametersAsBindings
                            .chain(specItem.specFunctionResultParameters.map { it.patBinding })
                            .toList()
                    }
                    else -> emptyList()
                }
            }
            is MvSchema -> owner.fieldsAsBindings
            else -> emptyList()
        }
        for (binding in bindings) {
            val bindingContext = binding.bindingOwner
            val ty = when (bindingContext) {
                null -> TyUnknown
                is MvFunctionParameter -> bindingContext.type?.loweredType(msl) ?: TyUnknown
                is MvSchemaFieldStmt -> bindingContext.type?.loweredType(msl) ?: TyUnknown
                else -> {
                    debugErrorOrFallback(
                        "${bindingContext.elementType} binding is not inferred",
                        TyUnknown
                    )
                }
            }
            this.ctx.writePatTy(binding, ty)
        }
    }

    fun inferCodeBlock(block: AnyCodeBlock): Ty =
        block.inferBlockType(Expectation.fromType(expectedReturnTy), coerce = true)

    fun inferSpecBlock(block: AnyCodeBlock): Ty =
        mslScope { block.inferBlockType(NoExpectation, coerce = false) }

    private fun AnyCodeBlock.inferBlockType(expected: Expectation, coerce: Boolean): Ty {
        val stmts = when (this) {
            is MvSpecCodeBlock, is MvModuleSpecBlock -> {
                // reorder stmts, move let stmts to the top, then let post, then others
                this.stmtList.sortedBy {
                    when {
                        it is MvLetStmt && !it.post -> 0
                        it is MvLetStmt && it.post -> 1
                        else -> 2
                    }
                }
            }
            else -> this.stmtList
        }
        stmts.forEach { processStmt(it) }

        val tailExpr = this.expr
        val expectedTy = expected.ty(ctx)
        return if (tailExpr == null) {
            if (coerce && expectedTy != null) {
                coerceTypes(this.rBrace ?: this, TyUnit, expectedTy)
            }
            TyUnit
        } else {
            if (coerce && expectedTy != null) {
                tailExpr.inferTypeCoerceTo(expectedTy)
            } else {
                tailExpr.inferType(expectedTy)
            }
        }
    }

    fun resolveTypeVarsIfPossible(ty: Ty): Ty = ctx.resolveTypeVarsIfPossible(ty)

    private fun processStmt(stmt: MvStmt) {
        when (stmt) {
            is MvLetStmt -> {
                val explicitTy = stmt.type?.loweredType(msl)
                val expr = stmt.initializer?.expr
                val pat = stmt.pat
                val initializerTy =
                    if (expr != null) {
                        val initializerTy = expr.inferType(explicitTy)
                        when {
                            explicitTy != null -> {
//                                coerceTypes(expr, initializerTy, explicitTy);
//                                explicitTy
                                if (coerceTypes(expr, initializerTy, explicitTy)) {
                                    explicitTy
                                } else {
                                    initializerTy
                                }
                            }
                            else -> initializerTy
                        }
                    } else {
                        pat?.anonymousTyVar() ?: TyUnknown
                    }
                pat?.collectBindings(
                    this,
                    explicitTy ?: resolveTypeVarsIfPossible(initializerTy)
                )
            }
            is MvSchemaFieldStmt -> {
                val binding = stmt.patBinding
                val ty = stmt.type?.loweredType(msl) ?: TyUnknown
                ctx.writePatTy(binding, resolveTypeVarsIfPossible(ty))
            }
            is MvIncludeStmt -> inferIncludeStmt(stmt)
            is MvUpdateSpecStmt -> inferUpdateStmt(stmt)
            is MvExprStmt -> stmt.expr.inferType()
            is MvSpecExprStmt -> stmt.expr.inferType()
            is MvPragmaSpecStmt -> {
                stmt.pragmaAttributeList.forEach { it.expr?.inferType() }
            }
        }
    }

    private fun MvExpr.inferType(expectedTy: Ty?): Ty = this.inferType(Expectation.fromType(expectedTy))

    private fun MvExpr.inferType(expected: Expectation = NoExpectation): Ty {
        try {
            return inferExprTy(this, expected)
        } catch (e: UnificationError) {
            if (e.context == null) {
                e.context = PsiErrorContext.fromElement(this)
            }
            throw e
        } catch (e: InferenceError) {
            if (e.context == null) {
                e.context = PsiErrorContext.fromElement(this)
            }
            throw e
        }
    }

    // returns inferred
    private fun MvExpr.inferTypeCoercableTo(expectedTy: Ty): Ty {
        val inferred = this.inferType(expectedTy)
        coerceTypes(this, inferred, expectedTy)
        return inferred
    }

    // returns inferred
    private fun MvExpr.inferTypeCoercableTo(expected: Expectation): Ty {
        val expectedTy = expected.ty(ctx)
        return if (expectedTy != null) {
            this.inferTypeCoercableTo(expectedTy)
        } else {
            this.inferType()
        }
    }

    // returns expected
    private fun MvExpr.inferTypeCoerceTo(expected: Ty): Ty {
        val actualTy = this.inferType(expected)
        return if (coerceTypes(this, actualTy, expected)) expected else actualTy
    }

    private fun inferExprTy(
        expr: MvExpr,
        expected: Expectation = NoExpectation
    ): Ty {
        ProgressManager.checkCanceled()
        if (ctx.isTypeInferred(expr)) error("Trying to infer expression type twice")

        val expectedTy = expected.ty(ctx)
        if (expectedTy != null) {
//            val expectedTy = this.ctx.resolveTypeVarsIfPossible(expectation.ty)
            when (expr) {
                is MvStructLitExpr,
                is MvPathExpr,
                is MvDotExpr,
                is MvCallExpr -> this.ctx.writeExprExpectedTy(expr, expectedTy)
            }
        }

        val exprTy = when (expr) {
            is MvPathExpr -> inferPathExprTy(expr, expected)
            is MvBorrowExpr -> inferBorrowExprTy(expr, expected)
            is MvCallExpr -> inferCallExprTy(expr, expected)
            is MvAssertMacroExpr -> inferMacroCallExprTy(expr)
            is MvStructLitExpr -> inferStructLitExprTy(expr, expected)
            is MvVectorLitExpr -> inferVectorLitExpr(expr, expected)
            is MvIndexExpr -> inferIndexExprTy(expr)

            is MvDotExpr -> inferDotExprTy(expr, expected)
            is MvDerefExpr -> inferDerefExprTy(expr)
            is MvLitExpr -> inferLitExprTy(expr)
            is MvTupleLitExpr -> inferTupleLitExprTy(expr, expected)
            is MvLambdaExpr -> inferLambdaExprTy(expr, expected)

            is MvMoveExpr -> expr.expr?.inferType() ?: TyUnknown
            is MvCopyExpr -> expr.expr?.inferType() ?: TyUnknown

            is MvSpecBlockExpr -> inferSpecBlockExprTy(expr)

            is MvCastExpr -> {
                expr.expr.inferType()
                val ty = expr.type.loweredType(msl)
                expected.ty(this.ctx)?.let {
                    this.ctx.combineTypes(it, ty)
                }
                ty
            }
            is MvIsExpr -> inferIsExprTy(expr)
            is MvParensExpr -> expr.expr?.inferType(expected) ?: TyUnknown
            is MvUnitExpr -> TyUnit

            is MvBinaryExpr -> inferBinaryExprTy(expr)
            is MvBangExpr -> {
                expr.expr?.inferType(TyBool)
                TyBool
            }

            is MvIfExpr -> inferIfExprTy(expr, expected)
            is MvWhileExpr -> inferWhileExpr(expr)
            is MvLoopExpr -> inferLoopExpr(expr)
            is MvForExpr -> inferForExpr(expr)
            is MvReturnExpr -> {
                expr.expr?.inferTypeCoercableTo(expectedReturnTy)
                TyNever
            }
            is MvContinueExpr -> TyNever
            is MvBreakExpr -> TyNever
            is MvAbortExpr -> {
                expr.expr?.inferTypeCoercableTo(TyInteger.DEFAULT)
                TyNever
            }
            is MvCodeBlockExpr -> expr.codeBlock.inferBlockType(expected, coerce = true)
            is MvAssignmentExpr -> inferAssignmentExprTy(expr)
            is MvBoolSpecExpr -> inferBoolSpecExpr(expr)
            is MvQuantExpr -> inferQuantExprTy(expr)
            is MvRangeExpr -> inferRangeExprTy(expr)
            is MvModifiesSpecExpr -> {
                expr.expr?.inferType()
                TyUnit
            }
            is MvAbortsWithSpecExpr -> {
                expr.exprList.forEach { it.inferTypeCoercableTo(TyInteger.DEFAULT) }
                TyUnit
            }

            is MvMatchExpr -> inferMatchExprTy(expr)

            else -> inferenceErrorOrFallback(expr, TyUnknown)
        }

        val refinedExprTy = exprTy.refineForSpecs(msl)
        ctx.writeExprTy(expr, refinedExprTy)

        return this.resolveTypeVarsIfPossible(refinedExprTy)
//        return refinedExprTy
    }

    private fun inferBoolSpecExpr(expr: MvBoolSpecExpr): Ty {
        expr.expr?.inferTypeCoercableTo(TyBool)
        if (expr is MvAbortsIfSpecExpr) {
            expr.abortsIfWith?.expr?.inferTypeCoercableTo(TyInteger.DEFAULT)
        }
        return TyUnit
    }

    private fun inferPathExprTy(pathExpr: MvPathExpr, expected: Expectation): Ty {

        val expectedType = expected.ty(ctx)
        val item = resolvePathCached(pathExpr.path, expectedType) ?: return TyUnknown

        return when (item) {
            is MvPatBinding -> ctx.getBindingType(item)
            is MvConst -> item.type?.loweredType(msl) ?: TyUnknown
            is MvGlobalVariableStmt -> item.type?.loweredType(true) ?: TyUnknown
            is MvNamedFieldDecl -> item.type?.loweredType(msl) ?: TyUnknown
            is MvStruct, is MvEnum -> {
                if (project.moveSettings.enableIndexExpr && pathExpr.parent is MvIndexExpr) {
                    TyLowering.lowerPath(pathExpr.path, item, ctx.msl)
                } else {
                    // invalid statements
                    TyUnknown
                }
            }
            is MvEnumVariant -> {
                // MyEnum<u8>::MyVariant
                //   ^ we need this path to be able to handle explicit type parameters
                val enumItemPath = pathExpr.path.qualifier ?: pathExpr.path
                val baseTy = instantiatePath<TyAdt>(enumItemPath, item.enumItem)
                    ?: return TyUnknown
                baseTy
            }
            is MvModule -> TyUnknown
            // todo: when function values are landed, this thing should return TyCallable
            is MvFunctionLike -> TyUnknown
            else -> debugErrorOrFallback(
                "Referenced item ${item.elementType} " +
                        "of ref expr `${pathExpr.text}` at ${pathExpr.location} cannot be inferred into type",
                TyUnknown
            )
        }
    }

    fun resolvePathCached(
        path: MvPath,
        expectedType: Ty?
    ): MvNamedElement? {
        val entries = resolvePath(path, expectedType)
            .filter {
                val namedElement = it.element()
                if (namedElement is MvPatBinding) {
                    // filter out bindings which are resolvable to enum variants
                    if (ctx.resolvedBindings[namedElement] is MvEnumVariant) {
                        return@filter false
                    }
                }
                true
            }
        val resolveResults = entries.toPathResolveResults(contextElement = path)
        ctx.writePath(path, resolveResults)

        return resolveResults.singleOrNull { it.isVisible }
            ?.element
            ?.let { resolveAliases(it) }
    }

    private fun inferAssignmentExprTy(assignExpr: MvAssignmentExpr): Ty {
        val lhsTy = assignExpr.expr.inferType()
        assignExpr.initializer.expr?.inferTypeCoercableTo(lhsTy)
        return TyUnit
    }

    private fun inferIsExprTy(isExpr: MvIsExpr): Ty {
        val itemTy = isExpr.expr.inferType()
        for (pathType in isExpr.typeList.filterIsInstance<MvPathType>()) {
            resolvePathCached(pathType.path, itemTy)
        }
        return TyBool
    }

    private fun inferBorrowExprTy(borrowExpr: MvBorrowExpr, expected: Expectation): Ty {
        val innerExpr = borrowExpr.expr ?: return TyUnknown
        val expectedInnerTy = (expected.ty(ctx) as? TyReference)?.referenced
        val hint = Expectation.fromType(expectedInnerTy)

        val innerExprTy = innerExpr.inferType(expected = hint)
        val innerRefTy = when (innerExprTy) {
            is TyReference, is TyTuple -> {
                ctx.reportTypeError(TypeError.ExpectedNonReferenceType(innerExpr, innerExprTy))
                TyUnknown
            }
            else -> innerExprTy
        }

        val mutability = Mutability.valueOf(borrowExpr.isMut)
        return TyReference(innerRefTy, mutability, ctx.msl)
    }

    private fun inferLambdaExprTy(lambdaExpr: MvLambdaExpr, expected: Expectation): Ty {

        val paramTys = lambdaExpr.lambdaParameters.map {
            val paramTy = it.type?.loweredType(ctx.msl) ?: TyInfer.TyVar()
            ctx.writePatTy(it.patBinding, paramTy)
            paramTy
        }

        val lambdaTy = TyCallable(paramTys, TyInfer.TyVar(), CallKind.Lambda)

        this.ctx.lambdaExprs.add(lambdaExpr)
        this.ctx.lambdaExprTypes[lambdaExpr] = lambdaTy

        val expectedTy = expected.ty(this.ctx)
        if (expectedTy != null) {
            // error if not TyCallable
            coerceTypes(lambdaExpr, lambdaTy, expectedTy)
        }

        return lambdaTy
    }

    private fun inferCallExprTy(callExpr: MvCallExpr, expected: Expectation): Ty {
        val callTy = instantiateCallableTy(callExpr) ?: return TyUnknown

        val expectedArgTys = inferExpectedArgsTys(callTy, expected)
        coerceArgumentTypes(
            callExpr.argumentExprs.map { CallArg.Arg(it) },
            callTy.paramTypes,
            expectedArgTys,
        )

        writeCallableType(callExpr, callTy, method = false)

        return callTy.returnType
    }

    private fun instantiateCallableTy(callExpr: MvCallExpr): TyCallable? {
        val path = callExpr.path
        val namedItem = resolvePathCached(path, expectedType = null)
        val callableTy =
            when (namedItem) {
                is MvFunctionLike -> {
                    instantiatePath<TyCallable>(path, namedItem) ?: return null
                }
                is MvFieldsOwner -> {
                    val tupleFields = namedItem.tupleFields ?: return null
                    instantiateTupleFuncTy(tupleFields, path, namedItem) ?: return null
                }
                // lambdas
                is MvPatBinding -> {
                    ctx.getBindingType(namedItem) as? TyCallable
                        ?: TyCallable.fake(callExpr.valueArguments.size, CallKind.Lambda)
                }
                else -> TyCallable.fake(
                    callExpr.valueArguments.size,
                    CallKind.Function.fake(path.project)
                )
            }
        return callableTy
    }

    private fun writeCallableType(callable: MvCallable, funcTy: TyCallable, method: Boolean) {
        // callableType TyVar are meaningful mostly for "needs type annotation" error.
        // if value parameter is missing, we don't want to show that error, so we cover
        // unknown parameters with TyUnknown here
        ctx.freezeUnification {
            val valueArguments = callable.valueArguments
            val paramTypes = funcTy.paramTypes.drop(if (method) 1 else 0)
            for ((i, paramType) in paramTypes.withIndex()) {
                val argumentExpr = valueArguments.getOrNull(i)?.expr
                if (argumentExpr == null) {
                    paramType.deepVisitTyInfers {
                        ctx.combineTypes(it, TyUnknown); true
                    }
                }
            }
            ctx.callableTypes[callable] = ctx.resolveTypeVarsIfPossible(funcTy) as TyCallable
        }
    }

    fun writePatTy(psi: MvPat, ty: Ty): Unit =
        ctx.writePatTy(psi, ty)

    fun inferFieldLookupTy(receiverTy: Ty, fieldLookup: MvFieldLookup): Ty {
        val tyAdt =
            receiverTy.unwrapRefs() as? TyAdt ?: return TyUnknown

        if (!msl && fieldLookup.containingModule != tyAdt.item.module) {
//        if (!msl && !fieldLookup.isDeclaredInModule(tyAdt.adtItem.module)) {
            // fields invisible outside module they're declared in
            return TyUnknown
        }

        val fieldEntry = getFieldLookupResolveVariants(tyAdt.item)
            .filterByName(fieldLookup.referenceName)
            .singleOrNull()
        val field = fieldEntry?.element() as? MvFieldDecl
        ctx.resolvedFields[fieldLookup] = field

        val fieldType = field?.type ?: return TyUnknown
        return fieldType.loweredType(msl).substitute(tyAdt.substitution)
    }

    fun inferMethodCallTy(selfTy: Ty, methodCall: MvMethodCall, expected: Expectation): Ty {
        val methodEntries = getMethodResolveVariants(methodCall, selfTy, msl)
            .filterByName(methodCall.referenceName)
            .dropInvisibleEntries(contextElement = methodCall)

        val functionItem = methodEntries.namedElements().singleOrNull()
        ctx.resolvedMethodCalls[methodCall] = functionItem

        val baseTy =
            when (functionItem) {
                is MvFunction -> {
                    val itemTy = instantiatePath<TyCallable>(methodCall, functionItem)
                        ?: return TyUnknown
                    itemTy
                }
                else -> {
                    // 1 for `self`
                    TyCallable.fake(
                        1 + methodCall.valueArguments.size,
                        CallKind.Function.fake(methodCall.project)
                    )
                }
            }
        val methodTy = ctx.resolveTypeVarsIfPossible(baseTy) as TyCallable

        val expectedInputTys = inferExpectedArgsTys(methodTy, expected)

        val args = listOf(CallArg.Self(selfTy)) + methodCall.argumentExprs.map { CallArg.Arg(it) }
        coerceArgumentTypes(
            args,
            methodTy.paramTypes,
            expectedInputTys,
        )

        writeCallableType(methodCall, methodTy, method = true)

        return methodTy.returnType
    }

    sealed class CallArg {
        data class Self(val selfTy: Ty): CallArg()
        data class Arg(val expr: MvExpr?): CallArg()
    }

    private fun coerceArgumentTypes(args: List<CallArg>, declaredTys: List<Ty>, expectedTys: List<Ty>) {
        for ((i, arg) in args.withIndex()) {
            val declaredTy = declaredTys.getOrNull(i) ?: TyUnknown
//            val expectedArgTy = expectedTys.getOrNull(i) ?: declaredArgTy
            val expectedTy = resolveTypeVarsIfPossible(
                expectedTys.getOrNull(i) ?: declaredTy
            )
            when (arg) {
                is CallArg.Arg -> {
                    val argExpr = arg.expr ?: continue
                    val argExprTy = argExpr.inferType(expected = Expectation.fromType(expectedTy))
                    coerceTypes(argExpr, argExprTy, expectedTy)
                }
                is CallArg.Self -> {
                    // method already resolved, so autoborrow() should always succeed
                    val actualSelfTy = autoborrow(arg.selfTy, expectedTy)
                        ?: error("unreachable, as method call cannot be resolved if autoborrow fails")
                    ctx.combineTypes(actualSelfTy, expectedTy)
                }
            }
            ctx.combineTypes(declaredTy, expectedTy)
        }
    }

    fun <T: Ty> instantiatePath(
        methodOrPath: MvMethodOrPath,
        genericItem: MvGenericDeclaration
    ): T? {
        val namedItem = genericItem as MvNamedElement
        // item type from explicit type parameters
        @Suppress("UNCHECKED_CAST")
        val pathTy = TyLowering.lowerPath(methodOrPath, namedItem, msl) as? T
            ?: return null

        val typeParamToTyVarSubst = genericItem.tyVarsSubst
        // TyTypeParameter -> TyVar for every TypeParameter which is not explicit set
        @Suppress("UNCHECKED_CAST")
        return pathTy.substitute(typeParamToTyVarSubst) as T
    }

    private fun instantiateTupleFuncTy(
        tupleFields: MvTupleFields,
        path: MvPath,
        positionalFieldsOwner: MvFieldsOwner
    ): TyCallable? {
        val (genericItem, genericPath) = when (positionalFieldsOwner) {
            is MvStruct -> positionalFieldsOwner to path
            is MvEnumVariant -> {
                val qualifierPath = path.qualifier ?: return null
                positionalFieldsOwner.enumItem to qualifierPath
            }
            else -> error("exhaustive")
        }
        val parameterTypes = tupleFields.tupleFieldDeclList.map { it.type.loweredType(msl) }
        val returnType = TyAdt.valueOf(genericItem)

        val typeParamsSubst = genericItem.tyTypeParamsSubst
        val typeArgsSubst = genericPath.typeArgsSubst(genericItem, msl)
        val tupleTy =
            TyCallable(
                parameterTypes,
                returnType,
                CallKind.Function(genericItem, typeParamsSubst)
            )
                .substitute(typeArgsSubst)
//        val tupleTy = baseTupleTy.substitute(typeArgsSubst)

        val tyVarsSubst = genericItem.tyVarsSubst
        return tupleTy.substitute(tyVarsSubst) as TyCallable
    }

    fun inferMacroCallExprTy(macroExpr: MvAssertMacroExpr): Ty {
        val ident = macroExpr.identifier
        if (ident.text == "assert") {
            val formalInputTys = listOf(TyBool, TyInteger.default())
            coerceArgumentTypes(
                macroExpr.valueArguments.map { it.expr }.map { CallArg.Arg(it) },
                formalInputTys,
                emptyList(),
            )
        }
        return TyUnit
    }

    private fun inferSpecBlockExprTy(specBlockExpr: MvSpecBlockExpr): Ty {
        val specBlock = specBlockExpr.specBlock
        if (specBlock != null) {
            inferSpecBlock(specBlock)
        }
        return TyUnit
    }

    /**
     * Unifies the output type with the expected type early, for more coercions
     * and forward type information on the input expressions
     */
    private fun inferExpectedArgsTys(callTy: TyCallable, expected: Expectation): List<Ty> {
        val expectedTy = expected.ty(ctx) ?: return emptyList()
        val declaredTy = resolveTypeVarsIfPossible(callTy.returnType)
        return if (ctx.combineTypes(expectedTy, declaredTy).isOk) {
            // resolve all arguments after every combine to check them based on one another
            callTy.paramTypes.map { ctx.resolveTypeVarsIfPossible(it) }
        } else {
            emptyList()
        }
//        return ctx.freezeUnification {
//            if (ctx.combineTypes(expectedTy, declaredTy).isOk) {
//                // resolve all arguments after every combine to check them based on one another
//                callTy.paramTypes.map { ctx.resolveTypeVarsIfPossible(it) }
//            } else {
//                emptyList()
//            }
//        }
    }

    fun inferAttrItem(attrItem: MvAttrItem) {
        val initializer = attrItem.attrItemInitializer
        if (initializer != null) initializer.expr?.let { inferExprTy(it) }
        for (innerAttrItem in attrItem.innerAttrItems) {
            inferAttrItem(innerAttrItem)
        }
    }

    @JvmName("inferType_")
    fun inferExprType(expr: MvExpr): Ty = expr.inferType()
    fun inferExprTypeCoercableTo(expr: MvExpr, expected: Ty): Ty = expr.inferTypeCoercableTo(expected)

    // combineTypes with errors
    fun coerceTypes(element: PsiElement, actual: Ty, expected: Ty): Boolean {
        val actual = resolveTypeVarsIfPossible(actual)
        val expected = resolveTypeVarsIfPossible(expected)
        if (actual === expected) {
            return true
        }
        val combineResult = ctx.combineTypes(actual, expected)
        return when (combineResult) {
            is RsResult.Ok -> true
            is RsResult.Err -> {
                reportTypeMismatch(combineResult.err, element, actual, expected)
                false
            }
        }
    }

    private fun inferDotExprTy(dotExpr: MvDotExpr, expected: Expectation): Ty {
        val receiverTy = ctx.resolveTypeVarsIfPossible(dotExpr.expr.inferType())

        val methodCall = dotExpr.methodCall
        val field = dotExpr.fieldLookup
        return when {
            field != null -> inferFieldLookupTy(receiverTy, field)
            methodCall != null -> inferMethodCallTy(receiverTy, methodCall, expected)
            // incomplete
            else -> TyUnknown
        }
    }

    fun inferStructLitExprTy(litExpr: MvStructLitExpr, expected: Expectation): Ty {
        val path = litExpr.path
        val expectedTy = expected.ty(ctx)

        val item = resolvePathCached(path, expectedTy) as? MvFieldsOwner
        if (item == null) {
            for (field in litExpr.fields) {
                field.expr?.inferType()
            }
            return TyUnknown
        }
        val structOrEnum = if (item is MvEnumVariant) item.enumItem else (item as MvStruct)

        var tyAdt = instantiatePath<TyAdt>(path, structOrEnum) ?: return TyUnknown
        if (expectedTy is TyAdt) {
            val expectedTySubst = expectedTy.substitution
            tyAdt.substitution.mapping.forEach { tyTypeParam, substTy ->
                // skip type parameters as we have no ability check
                if (substTy is TyTypeParameter) return@forEach

                val expectedSubstTy = expectedTySubst[tyTypeParam] ?: return@forEach
                // unifies if `substTy` is TyVar, performs type check if `substTy` is real type
                ctx.combineTypes(substTy, expectedSubstTy)
            }
            // resolved tyAdt inner TyVars after combining with expectedTy
            tyAdt = ctx.resolveTypeVarsIfPossible(tyAdt) as TyAdt
        }

        val namedFieldEntries = item.namedFields.asEntries()
        litExpr.fields.forEach { field ->
            val litFieldName = field.referenceName
            val namedField = namedFieldEntries
                .filterByName(litFieldName).singleItemOrNull() as? MvNamedFieldDecl
            val declaredFieldTy = namedField?.type?.loweredType(msl)
            val fieldTy = declaredFieldTy?.substitute(tyAdt.substitution) ?: TyUnknown
            val expr = field.expr
            if (expr != null) {
                expr.inferTypeCoercableTo(fieldTy)
            } else {
                val binding = getEntriesFromWalkingScopes(field, NAMES)
                    .filterByName(litFieldName)
                    .singleItemOrNull()
                val bindingTy = (binding as? MvPatBinding)
                    ?.let { ctx.getBindingType(it) } ?: TyUnknown
                coerceTypes(field, bindingTy, fieldTy)
            }
        }
        return tyAdt
    }

    private fun inferSchemaLitTy(schemaLit: MvSchemaLit): Ty {
        val path = schemaLit.path
        val schemaItem = path.maybeSchema
        if (schemaItem == null) {
            for (field in schemaLit.fields) {
                field.expr?.inferType()
            }
            return TyUnknown
        }

        val schemaTy = instantiatePath<TySchema>(path, schemaItem) ?: return TyUnknown
        schemaLit.fields.forEach { field ->
            val fieldTy = field.type(msl)?.substitute(schemaTy.substitution) ?: TyUnknown
            val expr = field.expr

            if (expr != null) {
                expr.inferTypeCoercableTo(fieldTy)
            } else {
                val bindingTy = field.resolveToBinding()?.let { ctx.getBindingType(it) } ?: TyUnknown
                coerceTypes(field, bindingTy, fieldTy)
            }
        }
        return schemaTy
    }

    private fun MvSchemaLitField.type(msl: Boolean) =
        this.resolveToDeclaration()?.type?.loweredType(msl)

    fun inferVectorLitExpr(litExpr: MvVectorLitExpr, expected: Expectation): Ty {
        val tyVar = TyInfer.TyVar()
        val explicitTy = litExpr.typeArgument?.type?.loweredType(msl)
        if (explicitTy != null) {
            ctx.combineTypes(tyVar, explicitTy)
        }

        val itemTy = ctx.resolveTypeVarsIfPossible(tyVar)
        val argExprs = litExpr.vectorLitItems.exprList
        val declaredArgTys = litExpr.vectorLitItems.exprList.map { itemTy }

        val litTy = TyCallable(declaredArgTys, TyVector(tyVar), CallKind.Lambda)
        val expectedArgTys =
            inferExpectedArgsTys(litTy, expected)
        coerceArgumentTypes(
            argExprs.map { CallArg.Arg(it) },
            declaredArgTys,
            expectedArgTys,
        )

        val vectorTy = ctx.resolveTypeVarsIfPossible(TyVector(tyVar))
        return vectorTy
    }

    private fun inferIndexExprTy(indexExpr: MvIndexExpr): Ty {
        val baseTy = indexExpr.receiverExpr.inferType()
        val argTy = indexExpr.argExpr.inferType()

        // compiler v2 only in non-msl
        if (project.moveSettings.disabledMove2 && !ctx.msl) {
            return TyUnknown
        }

        val derefTy = this.resolveTypeVarsIfPossible(baseTy).unwrapRefs()
        return when {
            derefTy is TyVector -> {
                // argExpr can be either TyInteger or TyRange
                when (argTy) {
                    is TyRange -> derefTy
                    is TyInteger, is TyInfer.IntVar, is TyNum -> derefTy.item
                    else -> {
                        coerceTypes(indexExpr.argExpr, argTy, if (ctx.msl) TyNum else TyInteger.DEFAULT)
                        derefTy.item
                    }
                }
            }
            baseTy is TyAdt -> {
                coerceTypes(indexExpr.argExpr, argTy, TyAddress)
                baseTy
            }
            else -> {
                if (baseTy !is TyUnknown) {
                    // unresolved item
                    ctx.reportTypeError(TypeError.IndexingIsNotAllowed(indexExpr.receiverExpr, baseTy))
                }
                TyUnknown
            }
        }
    }

    private fun inferQuantExprTy(quantExpr: MvQuantExpr): Ty {
        quantExpr.quantBindings?.quantBindingList.orEmpty()
            .forEach {
                collectQuantBinding(it)
            }
        quantExpr.quantWhere?.expr?.inferTypeCoercableTo(TyBool)
        quantExpr.expr?.inferTypeCoercableTo(TyBool)
        return TyBool
    }

    private fun collectQuantBinding(quantBinding: MvQuantBinding) {
        val bindingPat = quantBinding.binding
        val ty = when (quantBinding) {
            is MvRangeQuantBinding -> {
                val rangeTy = quantBinding.expr?.inferType()
                when (rangeTy) {
                    is TyVector -> rangeTy.item
                    is TyRange -> TyInteger.DEFAULT
                    else -> TyUnknown
                }
            }
            is MvTypeQuantBinding -> quantBinding.type?.loweredType(true) ?: TyUnknown
            else -> error("unreachable")
        }
        this.ctx.writePatTy(bindingPat, ty)
    }

    private fun inferRangeExprTy(rangeExpr: MvRangeExpr): Ty {
        val fromTy = rangeExpr.fromExpr.inferType()
        rangeExpr.toExpr?.inferTypeCoercableTo(fromTy)
        return TyRange(fromTy)
    }

    private fun inferBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        return when (binaryExpr.binaryOp.op) {
            "<", ">", "<=", ">=" -> inferOrderingBinaryExprTy(binaryExpr)
            "==", "!=" -> inferEqualityBinaryExprTy(binaryExpr)
            "||", "&&", "==>", "<==>" -> inferLogicBinaryExprTy(binaryExpr)

            "+", "-", "*", "/", "%" -> inferArithmeticBinaryExprTy(binaryExpr, false)
            "^", "|", "&" -> inferBitOpsExprTy(binaryExpr, false)
            "<<", ">>" -> inferBitShiftsExprTy(binaryExpr, false)

            "+=", "-=", "*=", "/=", "%=" -> inferArithmeticBinaryExprTy(binaryExpr, true)
            "|=", "^=", "&=" -> inferBitOpsExprTy(binaryExpr, true)
            ">>=", "<<=" -> inferBitShiftsExprTy(binaryExpr, true)

            else -> TyUnknown
        }
    }

    private fun inferArithmeticBinaryExprTy(binaryExpr: MvBinaryExpr, compoundAssigment: Boolean): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftTy = leftExpr.inferType()
        if (!leftTy.supportsArithmeticOp()) {
            ctx.reportTypeError(TypeError.UnsupportedBinaryOp(leftExpr, leftTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightTy = rightExpr.inferType(leftTy)
            if (!rightTy.supportsArithmeticOp()) {
                ctx.reportTypeError(TypeError.UnsupportedBinaryOp(rightExpr, rightTy, op))
                typeErrorEncountered = true
            }

            if (!typeErrorEncountered && ctx.combineTypes(leftTy, rightTy).isErr) {
                ctx.reportTypeError(
                    TypeError.IncompatibleArgumentsToBinaryExpr(
                        binaryExpr,
                        leftTy,
                        rightTy,
                        op
                    )
                )
                typeErrorEncountered = true
            }
        }
        return if (typeErrorEncountered) TyUnknown else (if (compoundAssigment) TyUnit else leftTy)
    }

    private fun inferEqualityBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        val leftTy = ctx.resolveTypeVarsIfPossible(leftExpr.inferType())
        if (rightExpr != null) {
            val rightTy = ctx.resolveTypeVarsIfPossible(rightExpr.inferType())

            // if any of the types has TyUnknown and TyInfer, combineTyVar will fail
            // it only happens in buggy situation, but it's annoying for the users, so return if not in devMode
            if (!isDebugModeEnabled()) {
                if ((leftTy.hasTyUnknown || rightTy.hasTyUnknown)
                    && (leftTy.hasTyInfer || rightTy.hasTyInfer)
                ) {
                    return TyBool
                }
            }

            if (ctx.combineTypes(leftTy, rightTy).isErr) {
                ctx.reportTypeError(
                    TypeError.IncompatibleArgumentsToBinaryExpr(binaryExpr, leftTy, rightTy, op)
                )
            }
        }
        return TyBool
    }

    private fun inferOrderingBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftTy = leftExpr.inferType()
        if (!leftTy.supportsOrdering()) {
            ctx.reportTypeError(TypeError.UnsupportedBinaryOp(leftExpr, leftTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightTy = rightExpr.inferType()
            if (!rightTy.supportsOrdering()) {
                ctx.reportTypeError(TypeError.UnsupportedBinaryOp(rightExpr, rightTy, op))
                typeErrorEncountered = true
            }
            if (!typeErrorEncountered) {
                coerceTypes(rightExpr, rightTy, expected = leftTy)
            }
        }
        return TyBool
    }

    private fun inferLogicBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        leftExpr.inferTypeCoercableTo(TyBool)
        if (rightExpr != null) {
            rightExpr.inferTypeCoercableTo(TyBool)
        }
        return TyBool
    }

    private fun inferBitOpsExprTy(binaryExpr: MvBinaryExpr, compoundAssigment: Boolean): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        val leftTy = leftExpr.inferTypeCoercableTo(TyInteger.DEFAULT)
        if (rightExpr != null) {
            rightExpr.inferTypeCoercableTo(leftTy)
        }
        return if (compoundAssigment) TyUnit else leftTy
    }

    private fun inferBitShiftsExprTy(binaryExpr: MvBinaryExpr, compoundAssigment: Boolean): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        val leftTy = leftExpr.inferTypeCoercableTo(TyInteger.DEFAULT)
        if (rightExpr != null) {
            rightExpr.inferTypeCoercableTo(TyInteger.U8)
        }
        return if (compoundAssigment) TyUnit else leftTy
    }

    private fun Ty.supportsArithmeticOp(): Boolean {
        val ty = this
//        val ty = resolveTypeVarsWithObligations(this)
        return ty is TyInteger
                || ty is TyNum
                || ty is TyInfer.TyVar
                || ty is TyInfer.IntVar
                || ty is TyUnknown
                || ty is TyNever
    }

    private fun Ty.supportsOrdering(): Boolean {
        val ty = this
//        val ty = resolveTypeVarsIfPossible(this)
        return ty is TyInteger
                || ty is TyNum
                || ty is TyInfer.IntVar
                || ty is TyUnknown
                || ty is TyNever
    }

    private fun inferDerefExprTy(derefExpr: MvDerefExpr): Ty {
        val innerExpr = derefExpr.expr ?: return TyUnknown

        val innerExprTy = innerExpr.inferType()
        if (innerExprTy !is TyReference) {
            ctx.reportTypeError(TypeError.InvalidDereference(innerExpr, innerExprTy))
            return TyUnknown
        }
        return innerExprTy.referenced
    }

    private fun inferTupleLitExprTy(tupleExpr: MvTupleLitExpr, expected: Expectation): Ty {
        val types = tupleExpr.exprList.mapIndexed { i, itemExpr ->
            val expectedTy = (expected.ty(ctx) as? TyTuple)?.types?.getOrNull(i)
            if (expectedTy != null) {
                itemExpr.inferTypeCoerceTo(expectedTy)
            } else {
                itemExpr.inferType()
            }
        }
        return TyTuple(types)
    }

    private fun inferLitExprTy(litExpr: MvLitExpr): Ty {
        return when {
            litExpr.boolLiteral != null -> TyBool
            litExpr.addressLit != null -> TyAddress
            litExpr.integerLiteral != null || litExpr.hexIntegerLiteral != null -> {
                if (ctx.msl) return TyNum
                val literal = (litExpr.integerLiteral ?: litExpr.hexIntegerLiteral)!!
                return TyInteger.fromSuffixedLiteral(literal) ?: TyInfer.IntVar()
            }
            litExpr.byteStringLiteral != null -> TyByteString(ctx.msl)
            litExpr.hexStringLiteral != null -> TyHexString(ctx.msl)
            else -> TyUnknown
        }
    }

    private fun inferIfExprTy(ifExpr: MvIfExpr, expected: Expectation): Ty {
        ifExpr.condition?.expr?.inferTypeCoercableTo(TyBool)
        val actualIfTy =
            ifExpr.codeBlock?.inferBlockType(expected, coerce = true)
                ?: ifExpr.inlineBlock?.expr?.inferTypeCoercableTo(expected)
        val elseBlock = ifExpr.elseBlock ?: return TyUnit
        val actualElseTy =
            elseBlock.codeBlock?.inferBlockType(expected, coerce = true)
                ?: elseBlock.inlineBlock?.expr?.inferTypeCoercableTo(expected)

        val expectedElseTy = expected.ty(ctx) ?: actualIfTy ?: TyUnknown
        if (actualElseTy != null) {
            elseBlock.tailExpr?.let {
                coerceTypes(it, actualElseTy.unwrapRefs(), expectedElseTy.unwrapRefs())
                // special case: `if (true) &s else &mut s` shouldn't show type error
//                if (expectedElseTy is TyReference && actualElseTy is TyReference) {
//                    coerceTypes(it, actualElseTy.referenced, expectedElseTy.referenced)
//                } else {
//                    coerceTypes(it, actualElseTy, expectedElseTy)
//                }
            }
        }

        return intersectTypes(listOfNotNull(actualIfTy, actualElseTy))
    }

    private fun intersectTypes(types: List<Ty>): Ty {
        if (types.isEmpty()) return TyUnknown
        return types.reduce { acc, ty -> intersectTypes(acc, ty) }
    }

    private fun intersectTypes(ty1: Ty, ty2: Ty): Ty {
        return when {
            ty1 is TyNever -> ty2
            ty2 is TyNever -> ty1
            ty1 is TyUnknown -> ty2  // even if TyUnknown
            else -> {
                val isOk = ctx.combineTypes(ty1, ty2).isOk || ctx.combineTypes(ty2, ty1).isOk
                if (isOk) {
                    when {
                        ty1 is TyReference && ty2 is TyReference -> {
                            val minMut = ty1.mutability.intersect(ty2.mutability)
                            TyReference(ty1.referenced, minMut, ty1.msl || ty2.msl)
                        }
                        else -> ty1
                    }
                } else {
                    TyUnknown
                }
            }
        }
    }

    private fun inferWhileExpr(whileExpr: MvWhileExpr): Ty {
        val conditionExpr = whileExpr.condition?.expr
        if (conditionExpr != null) {
            conditionExpr.inferTypeCoercableTo(TyBool)
        }
        return inferLoopLikeBlock(whileExpr)
    }

    private fun inferLoopExpr(loopExpr: MvLoopExpr): Ty {
        return inferLoopLikeBlock(loopExpr)
    }

    private fun inferForExpr(forExpr: MvForExpr): Ty {
        val iterCondition = forExpr.forIterCondition
        if (iterCondition != null) {
            val rangeExpr = iterCondition.expr
            val bindingTy =
                if (rangeExpr != null) {
                    val rangeTy = rangeExpr.inferType() as? TyRange
                    rangeTy?.item ?: TyUnknown
                } else {
                    TyUnknown
                }
            val bindingPat = iterCondition.patBinding
            if (bindingPat != null) {
                this.ctx.writePatTy(bindingPat, bindingTy)
            }
        }
        return inferLoopLikeBlock(forExpr)
    }

    private fun inferLoopLikeBlock(loopLike: MvLoopLike): Ty {
        val codeBlock = loopLike.codeBlock
        val inlineBlockExpr = loopLike.inlineBlock?.expr
        val expected = Expectation.fromType(TyUnit)
        when {
            codeBlock != null -> codeBlock.inferBlockType(expected, coerce = false)
            inlineBlockExpr != null -> inlineBlockExpr.inferType(expected)
        }
        return TyNever
    }

    private fun inferMatchExprTy(matchExpr: MvMatchExpr): Ty {
        val matchArgExpr = matchExpr.matchArgument.expr ?: return TyUnknown
        val matchArgTy = ctx.resolveTypeVarsIfPossible(matchArgExpr.inferType())

        val arms = matchExpr.arms
        for (arm in arms) {
            arm.pat.collectBindings(this@TypePsiWalker, matchArgTy)
            arm.matchArmGuard?.expr?.inferType(TyBool)
            arm.expr?.inferType()
        }
        val armTypes = arms.mapNotNull { it.expr?.let(ctx::getExprType) }
        return intersectTypes(armTypes)
    }

    private fun inferIncludeStmt(includeStmt: MvIncludeStmt) {
        val includeItem = includeStmt.includeItem ?: return
        when (includeItem) {
            is MvSchemaIncludeItem -> inferSchemaLitTy(includeItem.schemaLit)
            is MvAndIncludeItem -> {
                includeItem.schemaLitList.forEach { inferSchemaLitTy(it) }
            }
            is MvIfElseIncludeItem -> {
                includeItem.condition.expr?.inferTypeCoercableTo(TyBool)
                includeItem.schemaLitList.forEach { inferSchemaLitTy(it) }
            }
            is MvImplyIncludeItem -> {
                includeItem.childOfType<MvExpr>()?.inferTypeCoercableTo(TyBool)
                inferSchemaLitTy(includeItem.schemaLit)
            }
            else -> error("unreachable")
        }
    }

    private fun inferUpdateStmt(updateStmt: MvUpdateSpecStmt) {
        updateStmt.exprList.forEach { it.inferType() }
    }

//    private fun MvPat.extractBindings(patTy: Ty) {
//        this.collectBindings(this@TypePsiWalker, patTy)
//    }

    private fun reportTypeMismatch(
        mismatchError: TypeMismatchError,
        element: PsiElement,
        inferred: Ty,
        expected: Ty
    ) {
        if (mismatchError.ty1.javaClass in IGNORED_TYS || mismatchError.ty2.javaClass in IGNORED_TYS) return
        if (expected is TyReference && inferred is TyReference &&
            (expected.containsTyOfClass(IGNORED_TYS) || inferred.containsTyOfClass(IGNORED_TYS))
        ) {
            // report errors with unknown types when &mut is needed, but & is present
            if (!(expected.mutability.isMut && !inferred.mutability.isMut)) {
                return
            }
        }
//        reportTypeMismatch(element, expected, inferred)
        reportTypeError(TypeError.TypeMismatch(element, expected, inferred))
    }

    fun reportTypeError(typeError: TypeError) = ctx.reportTypeError(typeError)

    companion object {
        // ignoring possible false-positives (it's only basic experimental type checking)

        val IGNORED_TYS: List<Class<out Ty>> = listOf(
            TyUnknown::class.java,
            TyInfer.TyVar::class.java,
//            TyTypeParameter::class.java,
        )
    }
}

private fun <T> TypeFoldable<T>.containsTyOfClass(classes: List<Class<*>>): Boolean =
    visitWith(object: TypeVisitor() {
        override fun visit(ty: Ty): Boolean =
            if (classes.any { it.isInstance(ty) }) true else ty.deepVisitWith(this)
    })

