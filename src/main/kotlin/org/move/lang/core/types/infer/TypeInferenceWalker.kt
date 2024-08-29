package org.move.lang.core.types.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.move.cli.settings.debugErrorOrFallback
import org.move.cli.settings.isDebugModeEnabled
import org.move.cli.settings.moveSettings
import org.move.ide.formatter.impl.location
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve.collectMethodOrPathResolveVariants
import org.move.lang.core.resolve.processAll
import org.move.lang.core.resolve.ref.NONE
import org.move.lang.core.resolve.resolveSingleResolveVariant
import org.move.lang.core.resolve2.processFieldLookupResolveVariants
import org.move.lang.core.resolve2.processMethodResolveVariants
import org.move.lang.core.resolve2.ref.InferenceCachedPathElement
import org.move.lang.core.resolve2.ref.ResolutionContext
import org.move.lang.core.resolve2.ref.resolveAliases
import org.move.lang.core.resolve2.ref.resolvePathRaw
import org.move.lang.core.resolve2.resolveBindingForFieldShorthand
import org.move.lang.core.types.infer.Expectation.NoExpectation
import org.move.lang.core.types.ty.*
import org.move.lang.core.types.ty.TyReference.Companion.autoborrow
import org.move.stdext.RsResult
import org.move.stdext.chain

class TypeInferenceWalker(
    val ctx: InferenceContext,
    val project: Project,
    private val returnTy: Ty
) {
    val msl: Boolean = ctx.msl

    fun <T> mslScope(action: () -> T): T {
        if (ctx.msl) return action()
        ctx.msl = true
        val snapshot = ctx.startSnapshot()
        try {
            return action()
        } finally {
            ctx.msl = false
            snapshot.rollback()
        }
    }

    fun extractParameterBindings(owner: MvInferenceContextOwner) {
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
            val bindingContext = binding.owner
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

    fun inferFnBody(block: AnyBlock): Ty = block.inferBlockCoercableTo(returnTy)
    fun inferSpec(block: AnyBlock): Ty =
        mslScope { block.inferBlockType(NoExpectation) }

    private fun AnyBlock.inferBlockCoercableTo(expectedTy: Ty): Ty {
        return this.inferBlockCoercableTo(Expectation.maybeHasType(expectedTy))
    }

    private fun AnyBlock.inferBlockCoercableTo(expected: Expectation): Ty {
        return this.inferBlockType(expected, coerce = true)
    }

    private fun AnyBlock.inferBlockType(
        expected: Expectation = NoExpectation,
        coerce: Boolean = false
    ): Ty {
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
        stmts.forEach { processStatement(it) }

        val tailExpr = this.expr
        val expectedTy = expected.onlyHasTy(ctx)
        return if (tailExpr == null) {
            if (coerce && expectedTy != null) {
                coerce(this.rBrace ?: this, TyUnit, expectedTy)
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

    private fun processStatement(stmt: MvStmt) {
        when (stmt) {
            is MvLetStmt -> {
                val explicitTy = stmt.type?.loweredType(msl)
                val expr = stmt.initializer?.expr
                val pat = stmt.pat
                val inferredTy =
                    if (expr != null) {
                        val inferredTy = expr.inferType(Expectation.maybeHasType(explicitTy))
                        val coercedTy = if (explicitTy != null && coerce(expr, inferredTy, explicitTy)) {
                            explicitTy
                        } else {
                            inferredTy
                        }
                        coercedTy
                    } else {
                        pat?.anonymousTyVar() ?: TyUnknown
                    }
                pat?.extractBindings(
                    this,
                    explicitTy ?: resolveTypeVarsIfPossible(inferredTy)
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

    private fun MvExpr.inferType(expected: Ty?): Ty = this.inferType(Expectation.maybeHasType(expected))

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
    private fun MvExpr.inferTypeCoercableTo(expected: Ty): Ty {
        val inferred = this.inferType(expected)
        coerce(this, inferred, expected)
        return inferred
    }

    // returns inferred
    private fun MvExpr.inferTypeCoercableTo(expected: Expectation): Ty {
        val expectedTy = expected.onlyHasTy(ctx)
        return if (expectedTy != null) {
            this.inferTypeCoercableTo(expectedTy)
        } else {
            this.inferType()
        }
    }

    // returns expected
    private fun MvExpr.inferTypeCoerceTo(expected: Ty): Ty {
        val inferred = this.inferType(expected)
        return if (coerce(this, inferred, expected)) expected else inferred
    }

    private fun inferExprTy(
        expr: MvExpr,
        expected: Expectation = NoExpectation
    ): Ty {
        ProgressManager.checkCanceled()
        if (ctx.isTypeInferred(expr)) error("Trying to infer expression type twice")

        expected.tyAsNullable(this.ctx)?.let {
            when (expr) {
                is MvStructLitExpr,
                is MvPathExpr,
                is MvDotExpr,
                is MvCallExpr -> this.ctx.writeExprExpectedTy(expr, it)
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
            is MvLitExpr -> inferLitExprTy(expr, expected)
            is MvTupleLitExpr -> inferTupleLitExprTy(expr, expected)
            is MvLambdaExpr -> inferLambdaExpr(expr, expected)

            is MvMoveExpr -> expr.expr?.inferType() ?: TyUnknown
            is MvCopyExpr -> expr.expr?.inferType() ?: TyUnknown

            is MvItemSpecBlockExpr -> expr.specBlock?.let { inferSpec(it) } ?: TyUnknown

            is MvCastExpr -> {
                expr.expr.inferType()
                val ty = expr.type.loweredType(msl)
                expected.onlyHasTy(this.ctx)?.let {
                    this.ctx.combineTypes(it, ty)
                }
                ty
            }
            is MvParensExpr -> expr.expr?.inferType(expected) ?: TyUnknown

            is MvBinaryExpr -> inferBinaryExprTy(expr)
            is MvBangExpr -> {
                expr.expr?.inferType(Expectation.maybeHasType(TyBool))
                TyBool
            }

            is MvIfExpr -> inferIfExprTy(expr, expected)
            is MvWhileExpr -> inferWhileExpr(expr)
            is MvLoopExpr -> inferLoopExpr(expr)
            is MvForExpr -> inferForExpr(expr)
            is MvReturnExpr -> {
                expr.expr?.inferTypeCoercableTo(returnTy)
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

        val refinedExprTy = exprTy.mslScopeRefined(msl)
        ctx.writeExprTy(expr, refinedExprTy)
        return refinedExprTy
    }

    private fun inferBoolSpecExpr(expr: MvBoolSpecExpr): Ty {
        expr.expr?.inferTypeCoercableTo(TyBool)
        if (expr is MvAbortsIfSpecExpr) {
            expr.abortsIfWith?.expr?.inferTypeCoercableTo(TyInteger.DEFAULT)
        }
        return TyUnit
    }

    private fun inferPathExprTy(pathExpr: MvPathExpr, expected: Expectation): Ty {
        // special-case `result` inside item spec
//        if (msl && refExpr.path.text == "result") {
//            val funcItem = refExpr.ancestorStrict<MvItemSpec>()?.funcItem
//            if (funcItem != null) {
//                return funcItem.rawReturnType(true)
//            }
//        }
//        val path = pathExpr.path
//        val resolveVariants = resolvePathRaw(path)
//        ctx.writePath(path, resolveVariants.map { ResolvedPath.from(it, path)})

        val expectedType = expected.onlyHasTy(ctx)
        val item = resolvePathElement(pathExpr, expectedType) ?: return TyUnknown

        val ty = when (item) {
            is MvPatBinding -> ctx.getBindingType(item)
            is MvConst -> item.type?.loweredType(msl) ?: TyUnknown
            is MvGlobalVariableStmt -> item.type?.loweredType(true) ?: TyUnknown
            is MvNamedFieldDecl -> item.type?.loweredType(msl) ?: TyUnknown
            is MvStruct -> {
                if (project.moveSettings.enableIndexExpr && pathExpr.parent is MvIndexExpr) {
                    TyLowering().lowerPath(pathExpr.path, item, ctx.msl)
                } else {
                    // invalid statements
                    TyUnknown
                }
            }
            is MvEnumVariant -> item.enumItem.declaredType(ctx.msl)
            is MvModule -> TyUnknown
            else -> debugErrorOrFallback(
                "Referenced item ${item.elementType} " +
                        "of ref expr `${pathExpr.text}` at ${pathExpr.location} cannot be inferred into type",
                TyUnknown
            )
        }
        return ty
    }

    fun resolvePathElement(
        pathElement: InferenceCachedPathElement,
        expectedType: Ty?
    ): MvNamedElement? {
        val path = pathElement.path
        val resolvedItems = resolvePathRaw(path, expectedType).map { ResolvedItem.from(it, path) }
        ctx.writePath(path, resolvedItems)
        // resolve aliases
        return resolvedItems.singleOrNull { it.isVisible }
            ?.element
            ?.let { resolveAliases(it) }
    }

    private fun inferAssignmentExprTy(assignExpr: MvAssignmentExpr): Ty {
        val lhsTy = assignExpr.expr.inferType()
        assignExpr.initializer.expr?.inferTypeCoercableTo(lhsTy)
        return TyUnit
    }

    private fun inferBorrowExprTy(borrowExpr: MvBorrowExpr, expected: Expectation): Ty {
        val innerExpr = borrowExpr.expr ?: return TyUnknown
        val expectedInnerTy = (expected.onlyHasTy(ctx) as? TyReference)?.referenced
        val hint = Expectation.maybeHasType(expectedInnerTy)

        val innerExprTy = innerExpr.inferType(expected = hint)
//        val innerExprTy = inferExprTy(innerExpr, hint)
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

    private fun inferLambdaExpr(lambdaExpr: MvLambdaExpr, expected: Expectation): Ty {
        val bindings = lambdaExpr.patBindingList
        val lambdaTy =
            (expected.onlyHasTy(this.ctx) as? TyLambda) ?: TyLambda.unknown(bindings.size)

        for ((i, binding) in lambdaExpr.patBindingList.withIndex()) {
            val ty = lambdaTy.paramTypes.getOrElse(i) { TyUnknown }
            ctx.writePatTy(binding, ty)
        }
        lambdaExpr.expr?.inferTypeCoercableTo(lambdaTy.returnType)
        return TyUnknown
    }

    private fun inferCallExprTy(callExpr: MvCallExpr, expected: Expectation): Ty {
        val path = callExpr.path
        val namedItem = resolvePathElement(callExpr, expectedType = null)
        val baseTy =
            when (namedItem) {
                is MvFunctionLike -> {
                    val (itemTy, _) = instantiateMethodOrPath<TyFunction>(path, namedItem)
                        ?: return TyUnknown
                    itemTy
                }
                is MvStruct -> {
                    if (namedItem.tupleFields == null) return TyUnknown
                    val (itemTy, _) = instantiateMethodOrPath<TyFunction>(path, namedItem)
                        ?: return TyUnknown
                    itemTy
                }
                is MvPatBinding -> {
                    ctx.getBindingType(namedItem) as? TyLambda
                        ?: TyFunction.unknownTyFunction(callExpr.project, callExpr.valueArguments.size)
                }
                else -> TyFunction.unknownTyFunction(callExpr.project, callExpr.valueArguments.size)
            }
        val funcTy = ctx.resolveTypeVarsIfPossible(baseTy) as TyCallable

        val expectedInputTys =
            expectedInputsForExpectedOutput(expected, funcTy.returnType, funcTy.paramTypes)

        inferArgumentTypes(
            funcTy.paramTypes,
            expectedInputTys,
            callExpr.argumentExprs.map { InferArg.ArgExpr(it) })

        writeCallableType(callExpr, funcTy, method = false)

        return funcTy.returnType
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
                    paramType.visitInferTys {
                        ctx.combineTypes(it, TyUnknown); true
                    }
                }
            }
            ctx.writeCallableType(callable, ctx.resolveTypeVarsIfPossible(funcTy as Ty))
        }
    }

    fun writePatTy(psi: MvPat, ty: Ty): Unit =
        ctx.writePatTy(psi, ty)

    fun getResolvedPath(path: MvPath): List<ResolvedItem> {
        return ctx.resolvedPaths[path] ?: emptyList()
    }

    fun inferFieldLookupTy(receiverTy: Ty, fieldLookup: MvFieldLookup): Ty {
        val tyAdt =
            receiverTy.derefIfNeeded() as? TyAdt ?: return TyUnknown

        val field =
            resolveSingleResolveVariant(fieldLookup.referenceName) {
                processFieldLookupResolveVariants(fieldLookup, tyAdt, msl, it)
//                processNamedFieldVariants(fieldLookup, tyAdt, msl, it)
            } as? MvFieldDecl
        ctx.resolvedFields[fieldLookup] = field

        val fieldTy = field?.type?.loweredType(msl)?.substitute(tyAdt.typeParameterValues)
        return fieldTy ?: TyUnknown
    }

    fun inferMethodCallTy(receiverTy: Ty, methodCall: MvMethodCall, expected: Expectation): Ty {

        val resolutionCtx = ResolutionContext(methodCall, isCompletion = false)
        val resolvedMethods =
            collectMethodOrPathResolveVariants(methodCall, resolutionCtx) {
                processMethodResolveVariants(methodCall, receiverTy, msl, it)
            }
        val genericItem =
            resolvedMethods.filter { it.isVisible }.mapNotNull { it.element as? MvNamedElement }.singleOrNull()
        ctx.resolvedMethodCalls[methodCall] = genericItem

        val baseTy =
            when (genericItem) {
                is MvFunction -> {
                    val (itemTy, _) = instantiateMethodOrPath<TyFunction>(methodCall, genericItem)
                        ?: return TyUnknown
                    itemTy
                }
                else -> {
                    // 1 for `self`
                    TyFunction.unknownTyFunction(methodCall.project, 1 + methodCall.valueArguments.size)
                }
            }
        val methodTy = ctx.resolveTypeVarsIfPossible(baseTy) as TyCallable

        val expectedInputTys =
            expectedInputsForExpectedOutput(expected, methodTy.returnType, methodTy.paramTypes)

        inferArgumentTypes(
            methodTy.paramTypes,
            expectedInputTys,
            listOf(InferArg.SelfType(receiverTy))
                    + methodCall.argumentExprs.map { InferArg.ArgExpr(it) }
        )

        writeCallableType(methodCall, methodTy, method = true)

        return methodTy.returnType
    }

    sealed class InferArg {
        data class SelfType(val selfTy: Ty): InferArg()
        data class ArgExpr(val expr: MvExpr?): InferArg()
    }

    private fun inferArgumentTypes(
        formalInputTys: List<Ty>,
        expectedInputTys: List<Ty>,
        inferArgs: List<InferArg>,
    ) {
        for ((i, inferArg) in inferArgs.withIndex()) {
            val formalInputTy = formalInputTys.getOrNull(i) ?: TyUnknown
            val expectedInputTy = expectedInputTys.getOrNull(i) ?: formalInputTy
            val expectation = Expectation.maybeHasType(expectedInputTy)
            val expectedTy =
                resolveTypeVarsIfPossible(expectation.onlyHasTy(ctx) ?: formalInputTy)
            when (inferArg) {
                is InferArg.ArgExpr -> {
                    val argExpr = inferArg.expr ?: continue
                    val argExprTy = argExpr.inferType(expected = expectation)
                    coerce(argExpr, argExprTy, expectedTy)
                    // retrieve obligations
                    ctx.combineTypes(formalInputTy, expectedTy)
                }
                is InferArg.SelfType -> {
                    // method already resolved, so autoborrow() should always succeed
                    val actualSelfTy = autoborrow(inferArg.selfTy, expectedTy)
                        ?: error("unreachable, as method call cannot be resolved if autoborrow fails")
                    ctx.combineTypes(actualSelfTy, expectedTy)
                }
            }

            // retrieve obligations
            ctx.combineTypes(formalInputTy, expectedTy)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: GenericTy> instantiateMethodOrPath(
        methodOrPath: MvMethodOrPath,
        genericItem: MvGenericDeclaration
    ): Pair<T, Substitution>? {
        var itemTy =
            this.ctx.methodOrPathTypes.getOrPut(methodOrPath) {
                // can only be method or path, both are resolved to MvNamedElement
                val genericNamedItem = genericItem as MvNamedElement
                TyLowering().lowerPath(methodOrPath, genericNamedItem, msl) as? T
                    ?: return null
            }

        val typeParameters = genericItem.tyInfers
        itemTy = itemTy.substitute(typeParameters) as? T ?: return null

        unifySubst(typeParameters, itemTy.substitution)
        return Pair(itemTy, typeParameters)
    }

    private fun unifySubst(subst1: Substitution, subst2: Substitution) {
        subst1.typeSubst.forEach { (k, v1) ->
            subst2[k]?.let { v2 ->
                if (k != v1 && v1 !is TyTypeParameter && v1 !is TyUnknown) {
                    ctx.combineTypes(v2, v1)
                }
            }
        }
    }

    private fun callableTy(tupleFields: MvTupleFields, item: MvFieldsOwner): TyFunction {
        val parameterTypes = tupleFields.tupleFieldDeclList.map { it.type.loweredType(msl) }
        val returnType = when (item) {
            is MvStruct -> item.declaredType(msl)
            is MvEnumVariant -> item.enumItem.declaredType(msl)
            else -> error("exhaustive")
        }
        val typeParams = (item as MvGenericDeclaration).tyTypeParams
        return TyFunction(
            item as MvGenericDeclaration,
            typeParams,
            paramTypes = parameterTypes,
            returnType = returnType,
            acquiresTypes = emptyList(),
        )
            .substitute(typeParams) as TyFunction
    }

    fun inferMacroCallExprTy(macroExpr: MvAssertMacroExpr): Ty {
        val ident = macroExpr.identifier
        if (ident.text == "assert") {
            val formalInputTys = listOf(TyBool, TyInteger.default())
            inferArgumentTypes(
                formalInputTys,
                emptyList(),
                macroExpr.valueArguments.map { it.expr }.map { InferArg.ArgExpr(it) }
            )
        }
        return TyUnit
    }

    /**
     * Unifies the output type with the expected type early, for more coercions
     * and forward type information on the input expressions
     */
    private fun expectedInputsForExpectedOutput(
        expectedRet: Expectation,
        formalRet: Ty,
        formalArgs: List<Ty>,
    ): List<Ty> {
        val resolvedFormalRet = resolveTypeVarsIfPossible(formalRet)
        val retTy = expectedRet.onlyHasTy(ctx) ?: return emptyList()
        // Rustc does `fudge` instead of `probe` here. But `fudge` seems useless in our simplified type inference
        // because we don't produce new type variables during unification
        // https://github.com/rust-lang/rust/blob/50cf76c24bf6f266ca6d253a/compiler/rustc_infer/src/infer/fudge.rs#L98
        return ctx.freezeUnification {
            if (ctx.combineTypes(retTy, resolvedFormalRet).isOk) {
                formalArgs.map { ctx.resolveTypeVarsIfPossible(it) }
            } else {
                emptyList()
            }
        }
    }

    fun inferAttrItem(attrItem: MvAttrItem) {
        val initializer = attrItem.attrItemInitializer
        if (initializer != null) initializer.expr?.let { inferExprTy(it) }
        for (innerAttrItem in attrItem.innerAttrItems) {
            inferAttrItem(innerAttrItem)
        }
    }

    @JvmName("inferType_")
    fun inferType(expr: MvExpr): Ty =
        expr.inferType()

    // combineTypes with errors
    fun coerce(element: PsiElement, inferred: Ty, expected: Ty): Boolean =
        coerceResolved(
            element,
            resolveTypeVarsIfPossible(inferred),
            resolveTypeVarsIfPossible(expected)
        )

    private fun coerceResolved(element: PsiElement, inferred: Ty, expected: Ty): Boolean {
        val coerceResult = ctx.tryCoerce(inferred, expected)
        return when (coerceResult) {
            is RsResult.Ok -> true
            is RsResult.Err -> when (val err = coerceResult.err) {
                is CombineTypeError.TypeMismatch -> {
                    checkTypeMismatch(err, element, inferred, expected)
                    false
                }
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
        val expectedType = expected.onlyHasTy(ctx)

        val item = resolvePathElement(litExpr, expectedType) as? MvFieldsOwner
        if (item == null) {
            for (field in litExpr.fields) {
                field.expr?.inferType()
            }
            return TyUnknown
        }

        val genericItem = if (item is MvEnumVariant) item.enumItem else (item as MvStruct)
        val (tyAdt, typeParameters) = instantiateMethodOrPath<TyAdt>(path, genericItem)
            ?: return TyUnknown
        expectedType?.let { expectedTy ->
            unifySubst(typeParameters, expectedTy.typeParameterValues)
        }

        litExpr.fields.forEach { field ->
            // todo: can be cached, change field reference impl
            val namedField =
                resolveSingleResolveVariant(field.referenceName) { it.processAll(NONE, item.namedFields) }
                        as? MvNamedFieldDecl
            val rawFieldTy = namedField?.type?.loweredType(msl)
            val fieldTy = rawFieldTy?.substitute(tyAdt.substitution) ?: TyUnknown
//            val fieldTy = field.type(msl)?.substitute(tyAdt.substitution) ?: TyUnknown
            val expr = field.expr
            if (expr != null) {
                expr.inferTypeCoercableTo(fieldTy)
            } else {
                val bindingTy = (resolveBindingForFieldShorthand(field).singleOrNull() as? MvPatBinding)
                    ?.let { ctx.getBindingType(it) }
                    ?: TyUnknown
//                val bindingTy = field.resolveToBinding()?.let { ctx.getBindingType(it) } ?: TyUnknown
                coerce(field, bindingTy, fieldTy)
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

        val (schemaTy, _) = instantiateMethodOrPath<TySchema>(path, schemaItem) ?: return TyUnknown
//        expected.onlyHasTy(ctx)?.let { expectedTy ->
//            ctx.unifySubst(typeParameters, expectedTy.typeParameterValues)
//        }

        schemaLit.fields.forEach { field ->
            val fieldTy = field.type(msl)?.substitute(schemaTy.substitution) ?: TyUnknown
            val expr = field.expr

            if (expr != null) {
                expr.inferTypeCoercableTo(fieldTy)
            } else {
                val bindingTy = field.resolveToBinding()?.let { ctx.getBindingType(it) } ?: TyUnknown
                coerce(field, bindingTy, fieldTy)
            }
        }
        return schemaTy
    }

    private fun MvSchemaLitField.type(msl: Boolean) =
        this.resolveToDeclaration()?.type?.loweredType(msl)

//    private fun MvStructLitField.type(msl: Boolean) =
//        this.resolveToDeclaration()?.type?.loweredType(msl)

    fun inferVectorLitExpr(litExpr: MvVectorLitExpr, expected: Expectation): Ty {
        val tyVar = TyInfer.TyVar()
        val explicitTy = litExpr.typeArgument?.type?.loweredType(msl)
        if (explicitTy != null) {
            ctx.combineTypes(tyVar, explicitTy)
        }

        val exprs = litExpr.vectorLitItems.exprList
        val formalInputs = generateSequence { ctx.resolveTypeVarsIfPossible(tyVar) }.take(exprs.size).toList()
        val expectedInputTys =
            expectedInputsForExpectedOutput(expected, TyVector(tyVar), formalInputs)
        inferArgumentTypes(
            formalInputs,
            expectedInputTys,
            exprs.map { InferArg.ArgExpr(it) }
        )

        val vectorTy = ctx.resolveTypeVarsIfPossible(TyVector(tyVar))
        return vectorTy
    }

    private fun inferIndexExprTy(indexExpr: MvIndexExpr): Ty {
        val receiverTy = indexExpr.receiverExpr.inferType()
        val argTy = indexExpr.argExpr.inferType()

        // compiler v2 only in non-msl
        if (!ctx.msl && !project.moveSettings.enableIndexExpr) {
            return TyUnknown
        }

        val derefTy = receiverTy.derefIfNeeded()
        return when {
            derefTy is TyVector -> {
                // argExpr can be either TyInteger or TyRange
                when (argTy) {
                    is TyRange -> derefTy
                    is TyInteger, is TyInfer.IntVar, is TyNum -> derefTy.item
                    else -> {
                        coerce(indexExpr.argExpr, argTy, if (ctx.msl) TyNum else TyInteger.DEFAULT)
                        TyUnknown
                    }
                }
            }
            receiverTy is TyAdt -> {
                coerce(indexExpr.argExpr, argTy, TyAddress)
                receiverTy
            }
            else -> {
                ctx.reportTypeError(TypeError.IndexingIsNotAllowed(indexExpr.receiverExpr, receiverTy))
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
            "+", "-", "*", "/", "%" -> inferArithmeticBinaryExprTy(binaryExpr)
            "==", "!=" -> inferEqualityBinaryExprTy(binaryExpr)
            "||", "&&", "==>", "<==>" -> inferLogicBinaryExprTy(binaryExpr)
            "^", "|", "&" -> inferBitOpsExprTy(binaryExpr)
            "<<", ">>" -> inferBitShiftsExprTy(binaryExpr)
            else -> TyUnknown
        }
    }

    private fun inferArithmeticBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftTy = leftExpr.inferType()
//        val leftTy = leftExpr.inferType()
        if (!leftTy.supportsArithmeticOp()) {
            ctx.reportTypeError(TypeError.UnsupportedBinaryOp(leftExpr, leftTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightTy = rightExpr.inferType()
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
        return if (typeErrorEncountered) TyUnknown else leftTy
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
                coerce(rightExpr, rightTy, expected = leftTy)
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

    private fun inferBitOpsExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        val leftTy = leftExpr.inferTypeCoercableTo(TyInteger.DEFAULT)
        if (rightExpr != null) {
            rightExpr.inferTypeCoercableTo(leftTy)
        }
        return leftTy
    }

    private fun inferBitShiftsExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        val leftTy = leftExpr.inferTypeCoercableTo(TyInteger.DEFAULT)
        if (rightExpr != null) {
            rightExpr.inferTypeCoercableTo(TyInteger.U8)
        }
        return leftTy
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
        val ty = resolveTypeVarsIfPossible(this)
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
            val expectedTy = (expected.onlyHasTy(ctx) as? TyTuple)?.types?.getOrNull(i)
            if (expectedTy != null) {
                itemExpr.inferTypeCoerceTo(expectedTy)
            } else {
                itemExpr.inferType()
            }
        }
        return TyTuple(types)
    }

    private fun inferLitExprTy(litExpr: MvLitExpr, expected: Expectation): Ty {
        val litTy =
            when {
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
//        expected.onlyHasTy(this.ctx)
//            ?.let {
//                coerce(litExpr, litTy, it)
//            }
        return litTy
    }

    private fun inferIfExprTy(ifExpr: MvIfExpr, expected: Expectation): Ty {
        ifExpr.condition?.expr?.inferTypeCoercableTo(TyBool)
        val actualIfTy =
//            ifExpr.codeBlock?.inferBlockType(expected, coerce = false)
//                ?: ifExpr.inlineBlock?.expr?.inferType(expected)
            ifExpr.codeBlock?.inferBlockType(expected, coerce = true)
                ?: ifExpr.inlineBlock?.expr?.inferTypeCoercableTo(expected)
        val elseBlock = ifExpr.elseBlock ?: return TyUnit
        val actualElseTy =
//            elseBlock.codeBlock?.inferBlockType(expected)
//                ?: elseBlock.inlineBlock?.expr?.inferType(expected)
            elseBlock.codeBlock?.inferBlockType(expected, coerce = true)
                ?: elseBlock.inlineBlock?.expr?.inferTypeCoercableTo(expected)

        val expectedElseTy = expected.onlyHasTy(ctx) ?: actualIfTy ?: TyUnknown
        if (actualElseTy != null) {
            elseBlock.tailExpr?.let {
                // special case: `if (true) &s else &mut s` shouldn't show type error
                if (expectedElseTy is TyReference && actualElseTy is TyReference) {
                    coerce(it, actualElseTy.referenced, expectedElseTy.referenced)
                } else {
                    coerce(it, actualElseTy, expectedElseTy)
                }
            }
        }

        return intersectTypes(listOfNotNull(actualIfTy, actualElseTy), symmetric = true)
    }

    private fun intersectTypes(types: List<Ty>, symmetric: Boolean = true): Ty {
        if (types.isEmpty()) return TyUnknown
        return types.reduce { acc, ty -> intersectTypes(acc, ty, symmetric) }
    }

    private fun intersectTypes(ty1: Ty, ty2: Ty, symmetric: Boolean = true): Ty {
        return when {
            ty1 is TyNever -> ty2
            ty2 is TyNever -> ty1
            ty1 is TyUnknown -> if (ty2 !is TyNever) ty2 else TyUnknown
            else -> {
                val ok = ctx.combineTypes(ty1, ty2).isOk
                        || if (symmetric) ctx.combineTypes(ty2, ty1).isOk else false
                if (ok) {
                    when {
                        ty1 is TyReference && ty2 is TyReference -> {
                            val minimalMut = ty1.mutability.intersect(ty2.mutability)
//                            val combined = ty1.permissions.intersect(ty2.permissions)
                            TyReference(ty1.referenced, minimalMut, ty1.msl || ty2.msl)
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
        val expected = Expectation.maybeHasType(TyUnit)
        when {
            codeBlock != null -> codeBlock.inferBlockType(expected)
            inlineBlockExpr != null -> inlineBlockExpr.inferType(expected)
        }
        return TyNever
    }

    private fun inferMatchExprTy(matchExpr: MvMatchExpr): Ty {
        val matchingTy = ctx.resolveTypeVarsIfPossible(matchExpr.matchArgument.expr.inferType())
        val arms = matchExpr.arms
        for (arm in arms) {
            arm.pat.extractBindings(matchingTy)
            arm.expr?.inferType()
            arm.matchArmGuard?.expr?.inferType(TyBool)
        }
        return intersectTypes(arms.mapNotNull { it.expr?.let(ctx::getExprType) })
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

    private fun MvPat.extractBindings(ty: Ty) {
        this.extractBindings(this@TypeInferenceWalker, ty)
    }

    private fun checkTypeMismatch(
        result: CombineTypeError.TypeMismatch,
        element: PsiElement,
        inferred: Ty,
        expected: Ty
    ) {
        if (result.ty1.javaClass in IGNORED_TYS || result.ty2.javaClass in IGNORED_TYS) return
        if (expected is TyReference && inferred is TyReference &&
            (expected.containsTyOfClass(IGNORED_TYS) || inferred.containsTyOfClass(IGNORED_TYS))
        ) {
            // report errors with unknown types when &mut is needed, but & is present
            if (!(expected.mutability.isMut && !inferred.mutability.isMut)) {
                return
            }
        }
        reportTypeMismatch(element, expected, inferred)
    }

    private fun reportTypeMismatch(element: PsiElement, expected: Ty, inferred: Ty) {
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
