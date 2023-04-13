package org.move.lang.core.types.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*
import org.move.stdext.RsResult

class TypeInferenceWalker(
    val ctx: InferenceContext,
    val msl: Boolean,
    private val returnTy: Ty
) {

    private val fulfillmentContext: FulfillmentContext = ctx.fulfill

    fun extractParameterBindings(owner: MvInferenceContextOwner) {
        val bindings = when (owner) {
            is MvFunction -> owner.allParamsAsBindings
            is MvSpecFunction -> owner.allParamsAsBindings
            is MvItemSpec -> owner.funcItem?.allParamsAsBindings.orEmpty()
            is MvSchema -> owner.fieldStmts.map { it.bindingPat }
            else -> emptyList()
        }
        for (binding in bindings) {
            val bindingContext = binding.owner
            val ty = when (bindingContext) {
                is MvFunctionParameter -> bindingContext.type?.loweredType(msl) ?: TyUnknown
                is MvSchemaFieldStmt -> bindingContext.annotationTy(ctx)
                else -> TyUnknown
            }
            this.ctx.writePatTy(binding, ty)
        }
    }

    fun inferFnBody(block: MvCodeBlock): Ty =
        block.inferTypeCoercableTo(returnTy)

    private fun MvCodeBlock.inferTypeCoercableTo(expected: Ty): Ty =
        inferCodeBlockTy(this, Expectation.ExpectHasType(expected), true)

    fun inferCodeBlockTy(block: MvCodeBlock, expected: Expectation, coerce: Boolean = false): Ty {
        for (stmt in block.stmtList) {
            processStatement(stmt)
//            blockCtx.processConstraints()
//            blockCtx.resolveTyVarsFromContext(blockCtx)
        }
        val tailExpr = block.expr

        val type = if (coerce && expected is Expectation.ExpectHasType) {
            tailExpr?.inferTypeCoercableTo(expected.ty)
        } else {
            tailExpr?.let { inferExprTy(it, ctx, expected) }
//            tailExpr?.inferType(expected)
        } ?: TyUnit
//        return if (isDiverging) TyNever else type
        return type

//        if (tailExpr == null) {
//            val expectedTy = expectation.onlyHasTy(ctx)
//            if (expectedTy != null && expectedTy !is TyUnit) {
//                reportTypeMismatch(block.rightBrace ?: block, expectedTy, TyUnit)
////                blockCtx.typeErrors.add(
////                    TypeError.TypeMismatch(
////                        block.rightBrace ?: block,
////                        expectedTy,
////                        TyUnit
////                    )
////                )
//                return TyUnknown
//            }
//            return TyUnit
//        } else {
//            val tailExprTy = inferExprTy(tailExpr, ctx, expectation)
////            blockCtx.processConstraints()
////            blockCtx.resolveTyVarsFromContext(blockCtx)
//            return tailExprTy
//        }
    }

    fun inferSpecBlock(block: MvItemSpecBlock, expectedTy: Ty? = null): Ty {
        for (stmt in block.stmtList) {
            processStatement(stmt)
//            ctx.processConstraints()
//            ctx.resolveTyVarsFromContext(ctx)
        }
        val tailExpr = block.expr
        if (tailExpr == null) {
            if (expectedTy != null && expectedTy !is TyUnit) {
                reportTypeMismatch(block.rBrace ?: block, expectedTy, TyUnit)
//                blockCtx.typeErrors.add(
//                    TypeError.TypeMismatch(
//                        block.rightBrace ?: block,
//                        expectedTy,
//                        TyUnit
//                    )
//                )
                return TyUnknown
            }
            return TyUnit
        } else {
            val tailExprTy = inferExprTy(tailExpr, ctx)
//            blockCtx.processConstraints()
//            blockCtx.resolveTyVarsFromContext(blockCtx)
            return tailExprTy
        }
    }

    private fun resolveTypeVarsWithObligations(ty: Ty): Ty {
        if (!ty.hasTyInfer) return ty
        val tyRes = ctx.resolveTypeVarsIfPossible(ty)
        if (!tyRes.hasTyInfer) return tyRes
        selectObligationsWherePossible()
        return ctx.resolveTypeVarsIfPossible(tyRes)
    }

    private fun selectObligationsWherePossible() {
        fulfillmentContext.selectWherePossible()
    }

    private fun processStatement(stmt: MvStmt) {
        when (stmt) {
            is MvLetStmt -> {
                val explicitTy = stmt.typeAnnotation?.type?.let { ctx.getTypeTy(it) }
                val expr = stmt.initializer?.expr

                val (inferredTy, coercedInferredTy) =
                    if (expr != null) {
                        val inferredTy = inferExprTy(expr, ctx, Expectation.maybeHasType(explicitTy))
                        val coercedTy = if (explicitTy != null && coerce(expr, inferredTy, explicitTy)) {
                            explicitTy
                        } else {
                            inferredTy
                        }
                        inferredTy to coercedTy
                    } else {
                        TyUnknown to TyInfer.TyVar()
                    }

//                val initializerTy =
//                    stmt.initializer?.expr?.let { inferExprTy(it, ctx, Expectation.maybeHasType(explicitTy)) }
//                val pat = stmt.pat ?: return
                stmt.pat?.extractBindings(
                    this,
                    explicitTy ?: resolveTypeVarsWithObligations(coercedInferredTy)
                )
//                val patTy = inferPatTy(pat, ctx, explicitTy ?: initializerTy)
//                val patTy = explicitTy ?: resolveTypeVarsWithObligations(initializerTy ?: TyUnknown)
//                collectBindings(pat, patTy, ctx)
            }
            is MvExprStmt -> inferExprTy(stmt.expr, ctx)
            is MvSpecExprStmt -> inferExprTy(stmt.expr, ctx)
        }
    }

    fun inferExprTy(
        expr: MvExpr,
        parentCtx: InferenceContext,
        expected: Expectation = Expectation.NoExpectation
    ): Ty {
        ProgressManager.checkCanceled()
        if (parentCtx.isTypeInferred(expr)) error("Trying to infer expression type twice")

        expected.tyAsNullable(ctx)?.let {
            when (expr) {
                is MvRefExpr, is MvDotExpr, is MvCallExpr -> ctx.writeExprExpectedTy(expr, it)
            }
        }

//        val itemContext =
//            expr.itemContextOwner?.itemContext(parentCtx.msl) ?: expr.project.itemContext(parentCtx.msl)
        var exprTy = when (expr) {
            is MvRefExpr -> inferRefExprTy(expr)
            is MvBorrowExpr -> inferBorrowExprTy(expr, parentCtx, expected)
            is MvCallExpr -> {
                inferCallExprTy(expr, parentCtx, expected)
//                val funcTy = inferCallExprTy(expr, parentCtx, expected) as? TyFunction
//                funcTy?.retType ?: TyUnknown
            }
            is MvMacroCallExpr -> {
                inferMacroCallExprTy(expr)
//                for (argumentExpr in expr.callArgumentExprs) {
//                    inferExprTy(argumentExpr, parentCtx)
//                }
//                TyUnknown
            }
            is MvStructLitExpr -> inferStructLitExprTy(expr, expected)
            is MvVectorLitExpr -> inferVectorLitExpr(expr, parentCtx)

            is MvDotExpr -> inferDotExprTy(expr)
            is MvDerefExpr -> inferDerefExprTy(expr, parentCtx)
            is MvLitExpr -> inferLitExprTy(expr, parentCtx)
            is MvTupleLitExpr -> inferTupleLitExprTy(expr, parentCtx)

            is MvMoveExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown
            is MvCopyExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown

            is MvItemSpecBlockExpr -> expr.specBlock?.let { inferSpecBlock(it) } ?: TyUnknown

            is MvCastExpr -> {
                inferExprTy(expr.expr, parentCtx, Expectation.NoExpectation)
                parentCtx.getTypeTy(expr.type)
            }
            is MvParensExpr -> expr.expr?.let { inferExprTy(it, parentCtx, expected) } ?: TyUnknown

            is MvBinaryExpr -> inferBinaryExprTy(expr, parentCtx)
            is MvBangExpr -> {
                expr.expr?.let { inferExprTy(it, parentCtx, Expectation.maybeHasType(TyBool)) }
                TyBool
            }

            is MvIfExpr -> inferIfExprTy(expr, expected)
            is MvWhileExpr -> inferWhileExpr(expr, parentCtx)
            is MvLoopExpr -> inferLoopExpr(expr, parentCtx)
            is MvReturnExpr -> {
                expr.expr?.inferTypeCoercableTo(returnTy)
//                val expectedReturnTy = expr.containingFunction?.let {
//                    (itemContext.getItemTy(it) as? TyFunction)?.retType
//                }
//                expr.expr?.let { inferExprTy(it, parentCtx, Expectation.maybeHasType(expectedReturnTy)) }
                TyNever
            }
            is MvCodeBlockExpr -> inferCodeBlockTy(expr.codeBlock, expected)
            is MvAssignmentExpr -> {
                val lhsExprTy = inferExprTy(expr.expr, parentCtx)
                expr.initializer.expr
                    ?.let { inferExprTy(it, parentCtx, Expectation.maybeHasType(lhsExprTy)) }
                TyUnit
            }
            else -> TyUnknown
        }
        if (expr.isMsl()) {
            exprTy = refineTypeForMsl(exprTy)
        }
//        if (exprTy is TyReference && expr.isMsl()) {
//            exprTy = exprTy.innermostTy()
//        }
//        if (exprTy is TyInteger && expr.isMsl()) {
//            exprTy = TyNum
//        }

//        if (expectedTy != null) {
//            ctx.combineTypes(expectedTy, exprTy)
////            parentCtx.registerEquateObligation(exprTy, expectedTy)
////            val compat = checkTysCompatible(expectedTy, exprTy, parentCtx.msl)
////            when (compat) {
////                is Compat.AbilitiesMismatch -> {
////                    parentCtx.typeErrors.add(TypeError.AbilitiesMismatch(expr, exprTy, compat.abilities))
////                }
////                is Compat.TypeMismatch -> {
////                    parentCtx.typeErrors.add(TypeError.TypeMismatch(expr, expectedTy, exprTy))
////                }
////                else -> parentCtx.registerEquateObligation(exprTy, expectedTy)
////            }
//        }

        parentCtx.writeExprTy(expr, exprTy)
        return exprTy
    }

    private fun MvExpr.inferType(expected: Ty?): Ty {
        return inferExprTy(this, ctx, Expectation.maybeHasType(expected))
    }

    private fun MvExpr.inferType(expected: Expectation = Expectation.NoExpectation): Ty {
        return inferExprTy(this, ctx, expected)
    }

    private fun MvExpr.inferTypeCoercableTo(expected: Ty): Ty {
        val inferred = inferExprTy(this, ctx, Expectation.maybeHasType(expected))
        return if (coerce(this, inferred, expected)) expected else inferred
    }

    private fun refineTypeForMsl(ty: Ty): Ty {
        var exprTy = ty
        if (ty is TyReference) {
            exprTy = ty.innermostTy()
        }
        if (exprTy is TyInteger) {
            exprTy = TyNum
        }
        return exprTy
    }

    private fun inferRefExprTy(refExpr: MvRefExpr): Ty {
        val item = refExpr.path.reference?.resolveWithAliases()
        val inferredTy = when (item) {
            is MvBindingPat -> ctx.getPatType(item)
            is MvConst -> {
                val itemContext = item.outerItemContext(ctx.msl)
                itemContext.getConstTy(item)
            }
            else -> TyUnknown
        }
        return inferredTy
    }

    private fun inferBorrowExprTy(borrowExpr: MvBorrowExpr, ctx: InferenceContext, expected: Expectation): Ty {
        val innerExpr = borrowExpr.expr ?: return TyUnknown
        val expectedInnerTy = (expected.onlyHasTy(ctx) as? TyReference)?.referenced
        val hint = Expectation.maybeHasType(expectedInnerTy)

        val innerExprTy = inferExprTy(innerExpr, ctx, hint)
        val mutabilities = RefPermissions.valueOf(borrowExpr.isMut)
        return TyReference(innerExprTy, mutabilities, ctx.msl)
    }

    private fun inferCallExprTy(
        callExpr: MvCallExpr,
        parentCtx: InferenceContext,
        expected: Expectation
    ): Ty {
        val path = callExpr.path
        val genericItem = path.reference?.resolveWithAliases() as? MvFunctionLike
        val baseTy =
            genericItem?.let { path.inferType(it).first } as? TyFunction2
                ?: unknownTyFunction(callExpr.valueArguments.size)
        val funcTy = resolveTypeVarsWithObligations(baseTy) as TyFunction2

        val expectedInputTys =
            expectedInputsForExpectedOutput(expected, funcTy.retType, funcTy.paramTypes)

        inferArgumentTypes(funcTy.paramTypes, expectedInputTys, callExpr.callArgumentExprs)

        parentCtx.writeAcquiredTypes(callExpr, funcTy.acquiresTypes)
        return funcTy.retType
    }

    private fun inferArgumentTypes(
        formalInputTys: List<Ty>,
        expectedInputTys: List<Ty>,
        argExprs: List<MvExpr>
    ) {
        for ((i, argExpr) in argExprs.withIndex()) {
            val formalInputTy = formalInputTys.getOrNull(i) ?: TyUnknown
            val expectedInputTy = expectedInputTys.getOrNull(i) ?: formalInputTy

            val expectation = Expectation.maybeHasType(expectedInputTy)
            val inferredTy = inferExprTy(argExpr, ctx, expectation)
            val coercedTy =
                resolveTypeVarsWithObligations(expectation.onlyHasTy(ctx) ?: formalInputTy)
            coerce(argExpr, inferredTy, coercedTy)

            // retrieve obligations
            ctx.combineTypes(formalInputTy, coercedTy)
        }
    }

    private fun unknownTyFunction(arity: Int): TyFunction2 =
        TyFunction2(generateSequence { TyUnknown }.take(arity).toList(), emptyList(), TyUnknown)


    fun inferMacroCallExprTy(macroExpr: MvMacroCallExpr): Ty {
        val ident = macroExpr.macroIdent.identifier
        if (ident.text == "assert") {
            val formalInputTys = listOf(TyBool, TyInteger.default())
            inferArgumentTypes(formalInputTys, emptyList(), macroExpr.callArgumentExprs)
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
        @Suppress("NAME_SHADOWING")
        val formalRet = resolveTypeVarsWithObligations(formalRet)
        val retTy = expectedRet.onlyHasTy(ctx) ?: return emptyList()
        // Rustc does `fudge` instead of `probe` here. But `fudge` seems useless in our simplified type inference
        // because we don't produce new type variables during unification
        // https://github.com/rust-lang/rust/blob/50cf76c24bf6f266ca6d253a/compiler/rustc_infer/src/infer/fudge.rs#L98
        return ctx.probe {
            if (ctx.combineTypes(retTy, formalRet).isOk) {
                formalArgs.map { ctx.resolveTypeVarsIfPossible(it) }
            } else {
                emptyList()
            }
        }
    }

//    fun getCombinedType(types: List<Pair<MvElement, Ty>>, coerce: Boolean = false): Ty {
//        if (types.isEmpty()) return TyUnknown
//        var truncatedTy: Ty? = null
//        for ((element, ty) in types) {
//            if (truncatedTy == null) {
//                truncatedTy = ty
//                continue
//            }
//            val coerced = if (coerce) coerce(element, ty, truncatedTy) else true
//            if (coerced) {
//
//            }
//        }
//        return types.reduce { acc, ty ->
//            if (coerce(acc, ty))
//        }
//    }

    // combineTypes with errors
    fun coerce(element: MvElement, inferred: Ty, expected: Ty): Boolean =
        coerceResolved(
            element,
            resolveTypeVarsWithObligations(inferred),
            resolveTypeVarsWithObligations(expected)
        )

    private fun coerceResolved(element: MvElement, inferred: Ty, expected: Ty): Boolean {
//        if (element is RsExpr) {
//            ctx.writeExpectedExprTyCoercable(element)
//        }
        val coerceResult = ctx.tryCoerce(inferred, expected)
        return when (coerceResult) {
            is RsResult.Ok -> true
            is RsResult.Err -> when (val err = coerceResult.err) {
                is CombineTypeError.TypeMismatch -> {
                    checkTypeMismatch(err, element, inferred, expected)
                    false
                }
                is CombineTypeError.AbilitiesMismatch -> false
            }
        }
    }

    private fun inferDotExprTy(dotExpr: MvDotExpr): Ty {
        val baseTy = resolveTypeVarsWithObligations(dotExpr.expr.inferType())
        val structTy = when (baseTy) {
            is TyReference -> baseTy.innermostTy() as? TyStruct2
            is TyStruct2 -> baseTy
            else -> null
        } ?: return TyUnknown

        val item = structTy.item
        val fieldName = dotExpr.structDotField.referenceName
        val fieldTy = item.fieldsMap[fieldName]
            ?.type
            ?.loweredType(msl)
            ?.substitute(structTy.typeParameterValues)
        return fieldTy ?: TyUnknown
    }

    fun inferStructLitExprTy(
        litExpr: MvStructLitExpr,
        expected: Expectation
    ): Ty {
        val path = litExpr.path
        val structItem = path.maybeStruct
        if (structItem == null) {
            for (field in litExpr.fields) {
                field.expr?.let { inferExprTy(it, ctx) }
            }
            return TyUnknown
        }

        val (structTy, typeParameters) = path.inferType(structItem, expected)
//        val subst = TyLowering.lowerPathGenerics(path, structItem, msl)
//        val typeParameters = ctx.instantiateBounds(structItem)
//        unifySubst(subst, typeParameters)
//        expected.onlyHasTy(ctx)?.let { expectedTy ->
//            unifySubst(typeParameters, expectedTy.typeParameterValues)
//        }

        litExpr.fields.forEach { field ->
            val fieldTy = field.type(msl)?.substitute(typeParameters) ?: TyUnknown
//            var fieldTy = field.resolveToDeclaration()
//                ?.type
//                ?.loweredType(msl)?.substitute() ?: return@forEach
            // replace TyTypeParameter with TyInfer
//            fieldTy = fieldTy.substitute(typeParameters)
            val expr = field.expr

            if (expr != null) {
                expr.inferTypeCoercableTo(fieldTy)
            } else {
                val bindingTy = field.resolveToBinding()?.let { ctx.getPatType(it) } ?: TyUnknown
                coerce(field, bindingTy, fieldTy)
            }
        }

//        val structTy = TyStruct2.valueOf(structItem).substitute(typeParameters)
        return structTy
//        return structTy.substituteTypeParameters(subst2)
    }

//    fun inferGenericItem(item): Ty {
//        val typeParameters = ctx.instantiateBounds(structItem)
//        expected.onlyHasTy(ctx)?.let { expectedTy ->
//            unifySubst(typeParameters, expectedTy.typeParameterValues)
//        }
//
//        litExpr.fields.forEach { field ->
//            val fieldTy = field.type(msl)?.substitute(typeParameters) ?: TyUnknown
////            var fieldTy = field.resolveToDeclaration()
////                ?.type
////                ?.loweredType(msl)?.substitute() ?: return@forEach
//            // replace TyTypeParameter with TyInfer
////            fieldTy = fieldTy.substitute(typeParameters)
//            val expr = field.expr
//
//            if (expr != null) {
//                expr.inferTypeCoercableTo(fieldTy)
//            } else {
//                val bindingTy = field.resolveToBinding()?.let { ctx.getPatType(it) } ?: TyUnknown
//                coerce(field, bindingTy, fieldTy)
//            }
//        }
//
//        val structTy = TyStruct2.valueOf(structItem).substitute(typeParameters)
//    }

    fun MvPath.inferType(
        genericItem: MvTypeParametersOwner,
        expected: Expectation = Expectation.NoExpectation
    ): Pair<Ty, Substitution> {
        val subst = TyLowering.lowerPathGenerics(this, genericItem, msl)
        val typeParameters = ctx.instantiateBounds(genericItem)
        unifySubst(subst, typeParameters)

        expected.onlyHasTy(ctx)?.let { expectedTy ->
            unifySubst(typeParameters, expectedTy.typeParameterValues)
        }
        val type = when (genericItem) {
            is MvStruct -> TyStruct2.valueOf(genericItem)
            is MvFunctionLike -> genericItem.type(msl)
            else -> TyUnknown
        }.substitute(typeParameters)
        return Pair(type, typeParameters)
    }

    private fun MvStructLitField.type(msl: Boolean) =
        this.resolveToDeclaration()?.type?.loweredType(msl)

//    private fun inferTypeType(type: MvType): BoundElement<MvE> {
//
//    }

//    private fun inferPath(path: MvPath): Ty {
//        // resolve to item
//        val item = path.reference?.resolveWithAliases() as? MvTypeParametersOwner ?: return TyUnknown
//
////        val (namedItem, subst) = TyLowering.lowerPathGenerics(path, item, emptySubstitution, msl)
////        val subst = item.generics.associateWith { TyInfer.TyVar(it) }.toTypeSubst()
//
////        val typeParameters = item?.generics.orEmpty()
////        val typeArguments = path.typeArguments.map { if (it is MvPathType) inferPath(it.path) else it }
//
//        if (path.typeArguments.isNotEmpty()) {
//            if (path.typeArguments.size != structTy.typeVars.size) return TyUnknown
//            for ((typeVar, typeArg) in structTy.typeVars.zip(path.typeArguments)) {
//                val typeArgTy = parentCtx.getTypeTy(typeArg.type)
//
//                // check compat for abilities
//                val compat = isCompatibleAbilities(typeVar, typeArgTy, path.isMsl())
//                val isCompat = when (compat) {
//                    is Compat.AbilitiesMismatch -> {
//                        parentCtx.typeErrors.add(
//                            TypeError.AbilitiesMismatch(
//                                typeArg,
//                                typeArgTy,
//                                compat.abilities
//                            )
//                        )
//                        false
//                    }
//
//                    else -> true
//                }
//                inferenceCtx.registerEquateObligation(typeVar, if (isCompat) typeArgTy else TyUnknown)
//            }
//        }
//    }

//    private val MvStructLitField.type: Ty get() = resolveToDeclaration()?.typeReference?.rawType ?: TyUnknown

    private fun unifySubst(subst1: Substitution, subst2: Substitution) {
        subst1.typeSubst.forEach { (k, v1) ->
            subst2[k]?.let { v2 ->
                if (k != v1 && v1 !is TyTypeParameter && v1 !is TyUnknown) {
                    ctx.combineTypes(v2, v1)
                }
            }
        }
//        subst1.constSubst.forEach { (k, c1) ->
//            subst2[k]?.let { c2 ->
//                if (k != c1 && c1 !is CtConstParameter && c1 !is CtUnknown) {
//                    ctx.combineConsts(c2, c1)
//                }
//            }
//        }
        // TODO take into account the lifetimes
    }

    fun inferVectorLitExpr(litExpr: MvVectorLitExpr, parentCtx: InferenceContext): Ty {
        var tyVector = TyVector(TyInfer.TyVar())

        val inferenceCtx = InferenceContext(parentCtx.msl, parentCtx.itemContext)
        val typeArgument = litExpr.typeArgument

        if (typeArgument != null) {
            val ty = parentCtx.getTypeTy(typeArgument.type)
            inferenceCtx.registerEquateObligation(tyVector.item, ty)
        }
        val exprs = litExpr.vectorLitItems.exprList
        for (expr in exprs) {
            inferenceCtx.processConstraints()
            tyVector = inferenceCtx.resolveTy(tyVector) as TyVector

            val exprTy = inferExprTy(expr, parentCtx, Expectation.maybeHasType(tyVector.item))
            inferenceCtx.registerEquateObligation(tyVector.item, exprTy)
        }
        inferenceCtx.processConstraints()
        tyVector = inferenceCtx.resolveTy(tyVector) as TyVector
        return tyVector
    }

//    fun inferLitFieldInitExprTy(
//        litField: MvStructLitField,
//        ctx: InferenceContext,
//        expected: Expectation
//    ): Ty {
//        val initExpr = litField.expr
//        return if (initExpr == null) {
//            val expectedTy = expected.onlyHasTy(ctx)
//            // find type of binding
//            val bindingPat =
//                litField.reference.multiResolve().filterIsInstance<MvBindingPat>().firstOrNull()
//                    ?: return TyUnknown
//            val bindingPatTy = ctx.getPatType(bindingPat)
//            if (expectedTy != null) {
//                if (!isCompatible(expectedTy, bindingPatTy, ctx.msl)) {
//                    ctx.typeErrors.add(TypeError.TypeMismatch(litField, expectedTy, bindingPatTy))
//                } else {
//                    ctx.registerEquateObligation(bindingPatTy, expectedTy)
//                }
//            }
//            bindingPatTy
//        } else {
//            // find type of expression
//            inferExprTy(initExpr, ctx, expected)
//        }
//    }

    private fun inferBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        return when (binaryExpr.binaryOp.op) {
            "<", ">", "<=", ">=" -> inferOrderingBinaryExprTy(binaryExpr, ctx)
            "+", "-", "*", "/", "%" -> inferArithmeticBinaryExprTy(binaryExpr, ctx)
            "==", "!=" -> inferEqualityBinaryExprTy(binaryExpr, ctx)
            "||", "&&" -> inferLogicBinaryExprTy(binaryExpr, ctx)
            "==>", "<==>" -> TyBool
            else -> TyUnknown
        }
    }

    private fun inferArithmeticBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftExprTy = inferExprTy(leftExpr, ctx)
        if (!leftExprTy.supportsArithmeticOp()) {
            ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(leftExpr, leftExprTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightExprTy = inferExprTy(rightExpr, ctx)
            if (!rightExprTy.supportsArithmeticOp()) {
                ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(rightExpr, rightExprTy, op))
                typeErrorEncountered = true
            }

            if (leftExprTy is TyInteger && rightExprTy is TyInteger) {
                val compat = isCompatibleIntegers(leftExprTy, rightExprTy)
                if (!compat) {
                    ctx.typeErrors.add(
                        TypeError.IncompatibleArgumentsToBinaryExpr(
                            binaryExpr,
                            leftExprTy,
                            rightExprTy,
                            op
                        )
                    )
                    typeErrorEncountered = true
                }
            }

            if (!typeErrorEncountered) {
                ctx.registerEquateObligation(leftExprTy, rightExprTy)
            }
        }
        return if (typeErrorEncountered) TyUnknown else leftExprTy
    }

    private fun inferEqualityBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        if (rightExpr != null) {
            val leftExprTy = inferExprTy(leftExpr, ctx)
            val rightExprTy = inferExprTy(rightExpr, ctx)
            if (!isCompatible(leftExprTy, rightExprTy)) {
                ctx.typeErrors.add(
                    TypeError.IncompatibleArgumentsToBinaryExpr(binaryExpr, leftExprTy, rightExprTy, op)
                )
            } else {
                ctx.registerEquateObligation(leftExprTy, rightExprTy)
            }
        }
        return TyBool
    }

    private fun inferOrderingBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftExprTy = inferExprTy(leftExpr, ctx)
        if (!leftExprTy.supportsOrdering()) {
            ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(leftExpr, leftExprTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightExprTy = inferExprTy(rightExpr, ctx)
            if (!rightExprTy.supportsOrdering()) {
                ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(rightExpr, rightExprTy, op))
                typeErrorEncountered = true
            }
            if (!typeErrorEncountered) {
                ctx.registerEquateObligation(leftExprTy, rightExprTy)
            }
        }

        return TyBool
    }

    private fun inferLogicBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right

        inferExprTy(leftExpr, ctx, Expectation.maybeHasType(TyBool))
        if (rightExpr != null) {
            inferExprTy(rightExpr, ctx, Expectation.maybeHasType(TyBool))
        }

        return TyBool
    }

    private fun Ty.supportsArithmeticOp(): Boolean {
        return this is TyInteger
                || this is TyNum
                || this is TyInfer.IntVar
                || this is TyUnknown
                || this is TyNever
    }

    private fun Ty.supportsOrdering(): Boolean {
        return this is TyInteger
                || this is TyNum
                || this is TyInfer.IntVar
                || this is TyUnknown
                || this is TyNever
    }

    private fun inferDerefExprTy(derefExpr: MvDerefExpr, ctx: InferenceContext): Ty {
        val exprTy =
            derefExpr.expr?.let { inferExprTy(it, ctx) }
        return (exprTy as? TyReference)?.referenced ?: TyUnknown
    }

    private fun inferTupleLitExprTy(tupleExpr: MvTupleLitExpr, ctx: InferenceContext): Ty {
        val types = tupleExpr.exprList.map { inferExprTy(it, ctx) }
        return TyTuple(types)
    }

    private fun inferLitExprTy(litExpr: MvLitExpr, ctx: InferenceContext): Ty {
        return when {
            litExpr.boolLiteral != null -> TyBool
            litExpr.addressLit != null -> TyAddress
            litExpr.integerLiteral != null || litExpr.hexIntegerLiteral != null -> {
                if (ctx.msl) return TyNum
                val literal = (litExpr.integerLiteral ?: litExpr.hexIntegerLiteral)!!
//            return TyInteger.fromSuffixedLiteral(literal) ?: TyInteger(TyInteger.DEFAULT_KIND)
                return TyInteger.fromSuffixedLiteral(literal) ?: TyInfer.IntVar()
            }

            litExpr.byteStringLiteral != null -> TyByteString(ctx.msl)
            litExpr.hexStringLiteral != null -> TyHexString(ctx.msl)
            else -> TyUnknown
        }
    }

    private fun inferIfExprTy(ifExpr: MvIfExpr, expected: Expectation): Ty {
        ifExpr.condition?.expr?.inferType(TyBool)

        val blockTys = mutableListOf<Ty?>()
        blockTys.add(ifExpr.codeBlock?.let { inferCodeBlockTy(it, expected) })
        blockTys.add(ifExpr.inlineBlock?.expr?.inferType(expected))

        val elseBlock = ifExpr.elseBlock
        if (elseBlock != null) {
            blockTys.add(elseBlock.codeBlock?.let { inferCodeBlockTy(it, expected) })
            blockTys.add(elseBlock.inlineBlock?.expr?.inferType(expected))
        }
        return if (elseBlock == null) TyUnit else intersectTypes(blockTys.filterNotNull(), symmetric = true)
//        return combineTys(ifExprTy, elseExprTy, ctx.msl)
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
                            val combined = ty1.permissions.intersect(ty2.permissions)
                            TyReference(ty1.referenced, combined, ty1.msl || ty2.msl)
                        }
                        else -> ty1
                    }
                } else {
                    TyUnknown
                }
            }
        }
    }

    private fun inferWhileExpr(whileExpr: MvWhileExpr, ctx: InferenceContext): Ty {
        val conditionExpr = whileExpr.condition?.expr
        if (conditionExpr != null) {
            inferExprTy(conditionExpr, ctx, Expectation.maybeHasType(TyBool))
        }
        val codeBlock = whileExpr.codeBlock
        val inlineBlockExpr = whileExpr.inlineBlock?.expr
        when {
            codeBlock != null -> {
                val blockCtx = ctx.childContext()
                inferCodeBlockTy(codeBlock, blockCtx, TyUnit)
            }
            inlineBlockExpr != null -> inferExprTy(inlineBlockExpr, ctx, Expectation.maybeHasType(TyUnit))
        }
        return TyUnit
    }

    private fun inferLoopExpr(loopExpr: MvLoopExpr, ctx: InferenceContext): Ty {
        val codeBlock = loopExpr.codeBlock
        val inlineBlockExpr = loopExpr.inlineBlock?.expr
        when {
            codeBlock != null -> {
                val blockCtx = ctx.childContext()
                inferCodeBlockTy(codeBlock, blockCtx, TyUnit)
            }
            inlineBlockExpr != null -> inferExprTy(inlineBlockExpr, ctx, Expectation.maybeHasType(TyUnit))
        }
        return TyNever
    }

    private fun checkTypeMismatch(
        result: CombineTypeError.TypeMismatch,
        element: MvElement,
        inferred: Ty,
        expected: Ty
    ) {
        if (result.ty1.javaClass in IGNORED_TYS || result.ty2.javaClass in IGNORED_TYS) return
        if (expected is TyReference && inferred is TyReference &&
            (expected.containsTyOfClass(IGNORED_TYS) || inferred.containsTyOfClass(IGNORED_TYS))
        ) {
            // report errors with unknown types when &mut is needed, but & is present
            if (!(expected.permissions.contains(RefPermissions.WRITE)
                        && !inferred.permissions.contains(RefPermissions.WRITE))
            ) {
                return
            }
        }
        reportTypeMismatch(element, expected, inferred)
    }

    // Another awful hack: check that inner expressions did not annotated as an error
    // to disallow annotation intersections. This should be done in a different way
    private fun reportTypeMismatch(element: PsiElement, expected: Ty, inferred: Ty) {
        if (ctx.typeErrors.all { !element.isAncestorOf(it.element) }) {
            ctx.addTypeError(TypeError.TypeMismatch(element, expected, inferred))
        }
    }

    companion object {
        // ignoring possible false-positives (it's only basic experimental type checking)

        val IGNORED_TYS: List<Class<out Ty>> = listOf(
            TyUnknown::class.java,
            TyInfer.TyVar::class.java,
            TyTypeParameter::class.java,
        )
    }
}
