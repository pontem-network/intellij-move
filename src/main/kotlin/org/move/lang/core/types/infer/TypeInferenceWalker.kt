package org.move.lang.core.types.infer

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*
import org.move.stdext.RsResult

class TypeInferenceWalker(
    val ctx: InferenceContext,
    private val returnTy: Ty
) {

    private val fulfillmentContext: FulfillmentContext = ctx.fulfill

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
                is MvSchemaFieldStmt -> bindingContext.type?.loweredType(msl) ?: TyUnknown
                else -> TyUnknown
            }
            this.ctx.writePatTy(binding, ty)
        }
    }

    fun inferFnBody(block: AnyBlock): Ty = block.inferBlockCoercableTo(returnTy)
    fun inferSpec(block: AnyBlock): Ty = mslScope { block.inferBlockType(Expectation.NoExpectation) }
//    fun inferFnBody(block: MvCodeBlock): Ty = block.inferBlockTypeCoercableTo(returnTy)

//    private fun MvCodeBlock.inferBlockTypeCoercableTo(expected: Ty): Ty =
//        this.inferAnyBlockTy(Expectation.ExpectHasType(expected), coerce = true)
//        inferCodeBlockTy(this, Expectation.ExpectHasType(expected), true)

//    private fun MvCodeBlock.inferBlockType(expected: Expectation, coerce: Boolean = false): Ty {
//        for (stmt in this.stmtList) {
//            processStatement(stmt)
//        }
//        val tailExpr = this.expr
//        val expectedTy = expected.onlyHasTy(ctx)
//        return if (tailExpr == null) {
//            if (coerce && expectedTy != null) {
//                // type error
//                reportTypeMismatch(this.rBrace ?: this, expectedTy, TyUnit)
////            blockCtx.typeErrors.add(
////                TypeError.TypeMismatch(
////                    block.rightBrace ?: block,
////                    expectedTy,
////                    TyUnit
////                )
//
//            }
//            TyUnit
//        } else {
//            if (coerce && expectedTy != null) {
//                tailExpr.inferTypeCoercableTo(expectedTy)
//            } else {
//                tailExpr.inferType(expectedTy)
//            }
//        }
////        val type = if (coerce && expected is Expectation.ExpectHasType) {
////            if (tailExpr == null) {
////
////            } else {
////                tailExpr.inferTypeCoercableTo(expected.ty)
////            }
////        } else {
////            tailExpr?.let { inferExprTy(it, ctx, expected) }
//////            tailExpr?.inferType(expected)
////        } ?: TyUnit
//////        return if (isDiverging) TyNever else type
////        return type
//    }

//    fun inferCodeBlockTy(block: MvCodeBlock, expected: Expectation, coerce: Boolean = false): Ty {
//
//    }

    private fun AnyBlock.inferBlockCoercableTo(expectedTy: Ty): Ty {
        return this.inferBlockCoercableTo(Expectation.maybeHasType(expectedTy))
    }

    private fun AnyBlock.inferBlockCoercableTo(expected: Expectation): Ty {
        return this.inferBlockType(expected, coerce = true)
    }

    private fun AnyBlock.inferBlockType(expected: Expectation, coerce: Boolean = false): Ty {
        for (stmt in this.stmtList) {
            processStatement(stmt)
        }
        val tailExpr = this.expr
        val expectedTy = expected.onlyHasTy(ctx)
        return if (tailExpr == null) {
            if (coerce && expectedTy != null) {
                coerce(this.rBrace ?: this, TyUnit, expectedTy)
            }
            TyUnit
        } else {
            if (coerce && expectedTy != null) {
                tailExpr.inferTypeCoercableTo(expectedTy)
            } else {
                tailExpr.inferType(expectedTy)
            }
        }
    }

//    fun inferBlockType(block: AnyBlock, expected: Expectation, coerce: Boolean = false): Ty {
//        for (stmt in block.stmtList) {
//            processStatement(stmt)
//        }
//        val tailExpr = block.expr
//        val expectedTy = expected.onlyHasTy(ctx)
//        return if (tailExpr == null) {
//            if (coerce && expectedTy != null) {
//                // type error
//                reportTypeMismatch(block.rBrace ?: block, expectedTy, TyUnit)
//            }
//            TyUnit
//        } else {
//            if (coerce && expectedTy != null) {
//                tailExpr.inferTypeCoercableTo(expectedTy)
//            } else {
//                tailExpr.inferType(expectedTy)
//            }
//        }
//    }

//    fun inferSpecBlockTy(block: MvItemSpecBlock, expectedTy: Ty? = null): Ty {
//        for (stmt in block.stmtList) {
//            processStatement(stmt)
//        }
//        val tailExpr = block.expr
//        if (tailExpr == null) {
//            if (expectedTy != null && expectedTy !is TyUnit) {
//                reportTypeMismatch(block.rBrace ?: block, expectedTy, TyUnit)
//                return TyUnknown
//            }
//            return TyUnit
//        } else {
//            return tailExpr.inferType(expectedTy)
//        }
//    }

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
                val explicitTy = stmt.type?.loweredType(msl)
//                val explicitTy = stmt.typeAnnotation?.type?.let { ctx.getTypeTy(it) }
                val expr = stmt.initializer?.expr

                val inferredTy =
                    if (expr != null) {
                        val inferredTy = inferExprTy(expr, ctx, Expectation.maybeHasType(explicitTy))
                        val coercedTy = if (explicitTy != null && coerce(expr, inferredTy, explicitTy)) {
                            explicitTy
                        } else {
                            inferredTy
                        }
                        coercedTy
                    } else {
                        TyInfer.TyVar()
                    }
                stmt.pat?.extractBindings(
                    this,
                    explicitTy ?: resolveTypeVarsWithObligations(inferredTy)
                )
            }
            is MvSchemaFieldStmt -> {
                val binding = stmt.bindingPat
                val ty = stmt.type?.loweredType(msl) ?: TyUnknown
                ctx.writePatTy(binding, resolveTypeVarsWithObligations(ty))
            }
            is MvExprStmt -> stmt.expr.inferType()
            is MvSpecExprStmt -> stmt.expr.inferType()
        }
    }

    private fun inferExprTy(
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

        val exprTy = when (expr) {
            is MvRefExpr -> inferRefExprTy(expr)
            is MvBorrowExpr -> inferBorrowExprTy(expr, parentCtx, expected)
            is MvCallExpr -> inferCallExprTy(expr, parentCtx, expected)
            is MvMacroCallExpr -> inferMacroCallExprTy(expr)
            is MvStructLitExpr -> inferStructLitExprTy(expr, expected)
            is MvVectorLitExpr -> inferVectorLitExpr(expr, expected)

            is MvDotExpr -> inferDotExprTy(expr)
            is MvDerefExpr -> inferDerefExprTy(expr, parentCtx)
            is MvLitExpr -> inferLitExprTy(expr)
            is MvTupleLitExpr -> inferTupleLitExprTy(expr, parentCtx)

            is MvMoveExpr -> expr.expr?.inferType() ?: TyUnknown
            is MvCopyExpr -> expr.expr?.inferType() ?: TyUnknown

            is MvItemSpecBlockExpr -> expr.specBlock?.let { inferSpec(it) } ?: TyUnknown

            is MvCastExpr -> {
                expr.expr.inferType()
                val ty = expr.type.loweredType(msl)
                expected.onlyHasTy(ctx)?.let {
                    ctx.combineTypes(it, ty)
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
            is MvReturnExpr -> {
                expr.expr?.inferTypeCoercableTo(returnTy)
                TyNever
            }
            is MvCodeBlockExpr -> expr.codeBlock.inferBlockType(expected)
            is MvAssignmentExpr -> {
                val lhsExprTy = expr.expr.inferType()
                expr.initializer.expr?.inferTypeCoercableTo(lhsExprTy)
                TyUnit
            }
            else -> TyUnknown
        }
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

        val refinedExprTy = exprTy.mslScopeRefined(msl)
        parentCtx.writeExprTy(expr, refinedExprTy)
        return refinedExprTy
    }

    private fun MvExpr.inferType(expected: Ty?): Ty {
        return inferExprTy(this, ctx, Expectation.maybeHasType(expected))
    }

    private fun MvExpr.inferType(expected: Expectation = Expectation.NoExpectation): Ty {
        return inferExprTy(this, ctx, expected)
    }

    private fun MvExpr.inferTypeCoercableTo(expected: Ty): Ty {
        val inferred = this.inferType(expected)
//        val inferred = inferExprTy(this, ctx, Expectation.maybeHasType(expected))
        return if (coerce(this, inferred, expected)) expected else inferred
//        return if (coerce(this, inferred, expected)) expected else inferred
    }

    private fun inferRefExprTy(refExpr: MvRefExpr): Ty {
        val item = refExpr.path.reference?.resolveWithAliases()
        val inferredTy = when (item) {
            is MvBindingPat -> ctx.getPatType(item)
            is MvConst -> {
                item.type?.loweredType(msl) ?: TyUnknown
//                val itemContext = item.outerItemContext(ctx.msl)
//                itemContext.getConstTy(item)
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
            genericItem?.let { inferPath(path, it).first } as? TyFunction2
                ?: TyFunction2.unknownTyFunction(callExpr.project, callExpr.valueArguments.size)
        val funcTy = resolveTypeVarsWithObligations(baseTy) as TyFunction2

        val expectedInputTys =
            expectedInputsForExpectedOutput(expected, funcTy.retType, funcTy.paramTypes)

        inferArgumentTypes(funcTy.paramTypes, expectedInputTys, callExpr.callArgumentExprs)

        parentCtx.writeAcquiredTypes(callExpr, funcTy.acquiresTypes)
        parentCtx.writeCallExprType(callExpr, funcTy)

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
    fun coerce(element: PsiElement, inferred: Ty, expected: Ty): Boolean =
        coerceResolved(
            element,
            resolveTypeVarsWithObligations(inferred),
            resolveTypeVarsWithObligations(expected)
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
                is CombineTypeError.AbilitiesMismatch -> {
                    reportTypeError(TypeError.AbilitiesMismatch(element, inferred, err.abilities))
                    false
                }
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

        val (structTy, typeParameters) = inferPath(path, structItem, expected)
        litExpr.fields.forEach { field ->
            val fieldTy = field.type(msl)?.substitute(typeParameters) ?: TyUnknown
            val expr = field.expr

            if (expr != null) {
                expr.inferTypeCoercableTo(fieldTy)
            } else {
                val bindingTy = field.resolveToBinding()?.let { ctx.getPatType(it) } ?: TyUnknown
                coerce(field, bindingTy, fieldTy)
            }
        }

        return structTy
    }

    fun inferPath(
        path: MvPath,
        genericItem: MvTypeParametersOwner,
        expected: Expectation = Expectation.NoExpectation
    ): Pair<Ty, Substitution> {
        val itemTy = TyLowering.lowerPath(path, msl)

        val typeParameters = genericItem.tyInfers
        if (itemTy is GenericTy) {
            unifySubst(itemTy.substitution, typeParameters)
        }

//        val subst = TyLowering.lowerPathGenerics(path, genericItem, msl)
//        unifySubst(subst, typeParameters)

        expected.onlyHasTy(ctx)?.let { expectedTy ->
            unifySubst(typeParameters, expectedTy.typeParameterValues)
        }
//        val type = when (genericItem) {
//            is MvStruct -> TyStruct2.valueOf(genericItem)
//            is MvFunctionLike -> {
//                genericItem.declaredType(msl, typeParameters)
//            }
//            else -> TyUnknown
//        }.substitute(typeParameters)
        return Pair(itemTy.substitute(typeParameters), typeParameters)
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
    }

    fun inferVectorLitExpr(litExpr: MvVectorLitExpr, expected: Expectation): Ty {
        val tyVar = TyInfer.TyVar()
        val explicitTy = litExpr.typeArgument?.type?.loweredType(msl)
        if (explicitTy != null) {
            ctx.combineTypes(tyVar, explicitTy)
        }
//        val tyVector =
//            if (explicitTy != null) {
//                TyVector(explicitTy)
//            } else TyVector(tyVar)
//
//        expected.onlyHasTy(ctx)?.let {
//            ctx.combineTypes(tyVector, it)
//        }

        val exprs = litExpr.vectorLitItems.exprList
        val formalInputs = generateSequence { ctx.resolveTypeVarsIfPossible(tyVar) }.take(exprs.size).toList()
        val expectedInputTys =
            expectedInputsForExpectedOutput(expected, TyVector(tyVar), formalInputs)
        inferArgumentTypes(formalInputs, expectedInputTys, exprs)

        return ctx.resolveTypeVarsIfPossible(TyVector(tyVar))

//        for (expr in exprs) {
////            val exprTy = expr.inferType(explicitTy)
////            val exprTy = inferExprTy(expr, parentCtx, Expectation.maybeHasType(tyVector.item))
////            inferenceCtx.registerEquateObligation(tyVector.item, exprTy)
//        }

//        expected.onlyHasTy(ctx)?.let { expectedTy ->
//            unifySubst(typeParameters, expectedTy.typeParameterValues)
//        }

//        val subst = TyLowering.lowerPathGenerics(path, genericItem, msl)
//        unifySubst(subst, typeParameters)
//
//        val type = when (genericItem) {
//            is MvStruct -> TyStruct2.valueOf(genericItem)
//            is MvFunctionLike -> genericItem.type(msl)
//            else -> TyUnknown
//        }.substitute(typeParameters)
//        return Pair(type, typeParameters)

//        val path = litExpr.path
//        val structItem = path.maybeStruct
//        if (structItem == null) {
//            for (field in litExpr.fields) {
//                field.expr?.let { inferExprTy(it, ctx) }
//            }
//            return TyUnknown
//        }
//
//        val (structTy, typeParameters) = inferPathType(path, structItem, expected)
//        litExpr.fields.forEach { field ->
//            val fieldTy = field.type(msl)?.substitute(typeParameters) ?: TyUnknown
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
//        return structTy
//
//        var tyVector = TyVector(TyInfer.TyVar())
//
//        val inferenceCtx = InferenceContext(parentCtx.msl, parentCtx.itemContext)
//        val typeArgument = litExpr.typeArgument
//
//        if (typeArgument != null) {
//            val ty = typeArgument.type.loweredType(msl)
////            val ty = parentCtx.getTypeTy(typeArgument.type)
//            inferenceCtx.registerEquateObligation(tyVector.item, ty)
//        }
//        val exprs = litExpr.vectorLitItems.exprList
//        for (expr in exprs) {
////            inferenceCtx.processConstraints()
////            tyVector = inferenceCtx.resolveTy(tyVector) as TyVector
//
//            val exprTy = inferExprTy(expr, parentCtx, Expectation.maybeHasType(tyVector.item))
//            inferenceCtx.registerEquateObligation(tyVector.item, exprTy)
//        }
////        inferenceCtx.processConstraints()
////        tyVector = inferenceCtx.resolveTy(tyVector) as TyVector
//        return tyVector
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

    private fun inferBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        return when (binaryExpr.binaryOp.op) {
            "<", ">", "<=", ">=" -> inferOrderingBinaryExprTy(binaryExpr)
            "+", "-", "*", "/", "%" -> inferArithmeticBinaryExprTy(binaryExpr)
            "==", "!=" -> inferEqualityBinaryExprTy(binaryExpr)
            "||", "&&", "==>", "<==>" -> inferLogicBinaryExprTy(binaryExpr)
            "^", "|", "&", "<<", ">>" -> inferBitOpsExprTy(binaryExpr)
            else -> TyUnknown
        }
    }

    private fun inferArithmeticBinaryExprTy(binaryExpr: MvBinaryExpr): Ty {
        val leftExpr = binaryExpr.left
        val rightExpr = binaryExpr.right
        val op = binaryExpr.binaryOp.op

        var typeErrorEncountered = false
        val leftTy = leftExpr.inferType()
        if (!leftTy.supportsArithmeticOp()) {
            ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(leftExpr, leftTy, op))
            typeErrorEncountered = true
        }
        if (rightExpr != null) {
            val rightTy = rightExpr.inferType()
            if (!rightTy.supportsArithmeticOp()) {
                ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(rightExpr, rightTy, op))
                typeErrorEncountered = true
            }

            if (!typeErrorEncountered && ctx.combineTypes(leftTy, rightTy).isErr) {
                ctx.typeErrors.add(
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

        if (rightExpr != null) {
            val leftTy = inferExprTy(leftExpr, ctx)
            val rightTy = inferExprTy(rightExpr, ctx)
            if (ctx.combineTypes(leftTy, rightTy).isErr) {
                ctx.typeErrors.add(
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

        val leftTy = leftExpr.inferTypeCoercableTo(TyInteger.default())
        if (rightExpr != null) {
            rightExpr.inferTypeCoercableTo(leftTy)
        }
        return leftTy
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

    private fun inferLitExprTy(litExpr: MvLitExpr): Ty {
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
        ifExpr.condition?.expr?.inferTypeCoercableTo(TyBool)

        val ifTy =
            ifExpr.codeBlock?.inferBlockType(expected)
                ?: ifExpr.inlineBlock?.expr?.inferType(expected)

        val elseBlock = ifExpr.elseBlock ?: return TyUnit

        val resolvedIfTy = resolveTypeVarsWithObligations(ifTy ?: TyUnknown)
        val expectedElse = Expectation.maybeHasType(resolvedIfTy)
        val elseTy =
            elseBlock.codeBlock?.inferBlockType(expected)
                ?: elseBlock.inlineBlock?.expr?.inferType(expectedElse)
        if (ifTy != null && elseTy != null) {
            val element = elseBlock.codeBlock ?: elseBlock.inlineBlock!!
            // special case for references in if-else block
            if (ifTy is TyReference && elseTy is TyReference) {
                coerce(element, elseTy.referenced, ifTy.referenced)
            } else {
                coerce(element, elseTy, ifTy)
            }
        }
        return intersectTypes(listOfNotNull(ifTy, elseTy), symmetric = true)
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

    private fun inferWhileExpr(whileExpr: MvWhileExpr): Ty {
        val conditionExpr = whileExpr.condition?.expr
        if (conditionExpr != null) {
            conditionExpr.inferTypeCoercableTo(TyBool)
        }
        val codeBlock = whileExpr.codeBlock
        val inlineBlockExpr = whileExpr.inlineBlock?.expr

        val expected = Expectation.maybeHasType(TyUnit)
        when {
            codeBlock != null -> codeBlock.inferBlockType(expected)
            inlineBlockExpr != null -> inlineBlockExpr.inferType(expected)
        }
        return TyUnit
    }

    private fun inferLoopExpr(loopExpr: MvLoopExpr): Ty {
        val codeBlock = loopExpr.codeBlock
        val inlineBlockExpr = loopExpr.inlineBlock?.expr
        val expected = Expectation.maybeHasType(TyUnit)
        when {
            codeBlock != null -> codeBlock.inferBlockType(expected)
            inlineBlockExpr != null -> inlineBlockExpr.inferType(expected)
        }
        return TyNever
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
        reportTypeError(TypeError.TypeMismatch(element, expected, inferred))
//        if (ctx.typeErrors.all { !element.isAncestorOf(it.element) }) {
//            ctx.addTypeError(TypeError.TypeMismatch(element, expected, inferred))
//        }
    }

    // Another awful hack: check that inner expressions did not annotated as an error
    // to disallow annotation intersections. This should be done in a different way
    fun reportTypeError(typeError: TypeError) {
        val element = typeError.element
        if (ctx.typeErrors.all { !element.isAncestorOf(it.element) }) {
            ctx.addTypeError(typeError)
        }
    }

    companion object {
        // ignoring possible false-positives (it's only basic experimental type checking)

        val IGNORED_TYS: List<Class<out Ty>> = listOf(
            TyUnknown::class.java,
            TyInfer.TyVar::class.java,
//            TyTypeParameter::class.java,
        )
    }
}
