package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*

fun inferExprTy(expr: MvExpr, parentCtx: InferenceContext, expectedTy: Ty? = null): Ty {
    val existingTy = parentCtx.exprTypes[expr]
    if (existingTy != null) {
        return existingTy
    }

    var exprTy = when (expr) {
        is MvRefExpr -> inferRefExprTy(expr, parentCtx)
        is MvBorrowExpr -> inferBorrowExprTy(expr, parentCtx)
        is MvCallExpr -> {
            val funcTy = inferCallExprTy(expr, parentCtx, expectedTy) as? TyFunction
            funcTy?.retType ?: TyUnknown
        }
        is MvMacroCallExpr -> {
            for (argumentExpr in expr.callArgumentExprs) {
                inferExprTy(argumentExpr, parentCtx)
            }
            TyUnknown
        }
        is MvStructLitExpr -> inferStructLitExpr(expr, parentCtx, expectedTy)
        is MvVectorLitExpr -> inferVectorLitExpr(expr, parentCtx)

        is MvDotExpr -> inferDotExprTy(expr, parentCtx)
        is MvDerefExpr -> inferDerefExprTy(expr, parentCtx)
        is MvLitExpr -> inferLitExprTy(expr, parentCtx)
        is MvTupleLitExpr -> inferTupleLitExprTy(expr, parentCtx)

        is MvMoveExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown
        is MvCopyExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown

        is MvCastExpr -> inferTypeTy(expr.type, parentCtx)
        is MvParensExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown

        is MvBinaryExpr -> inferBinaryExprTy(expr, parentCtx)
        is MvBangExpr -> {
            expr.expr?.let { inferExprTy(it, parentCtx, TyBool) }
            TyBool
        }

        is MvIfExpr -> inferIfExprTy(expr, parentCtx, expectedTy)
        is MvWhileExpr -> inferLoopExpr(expr, parentCtx)
        is MvLoopExpr -> inferLoopExpr(expr, parentCtx)
        is MvReturnExpr -> {
            val fnReturnTy = expr.containingFunction?.returnTypeTy(parentCtx)
            expr.expr?.let { inferExprTy(it, parentCtx, fnReturnTy) }
            TyNever
        }
        is MvCodeBlockExpr -> {
            inferCodeBlockTy(expr.codeBlock, parentCtx.childContext(), expectedTy)
        }
        is MvAssignmentExpr -> {
            val lhsExprTy = inferExprTy(expr.expr, parentCtx, null)
            expr.initializer.expr?.let { inferExprTy(it, parentCtx, lhsExprTy) }
            TyUnit
        }
        else -> TyUnknown
    }
    if (exprTy is TyReference && expr.isMsl()) {
        exprTy = exprTy.innermostTy()
    }
    if (exprTy is TyInteger && expr.isMsl()) {
        exprTy = TyNum
    }
    if (expectedTy != null) {
        val compat = checkTysCompatible(expectedTy, exprTy, parentCtx.msl)
        when (compat) {
            is Compat.AbilitiesMismatch -> {
                parentCtx.typeErrors.add(TypeError.AbilitiesMismatch(expr, exprTy, compat.abilities))
            }
            is Compat.TypeMismatch -> {
                parentCtx.typeErrors.add(TypeError.TypeMismatch(expr, expectedTy, exprTy))
            }
            else -> parentCtx.addConstraint(exprTy, expectedTy)
        }
    }

    parentCtx.cacheExprTy(expr, exprTy)
    return exprTy
}

private fun inferRefExprTy(refExpr: MvRefExpr, ctx: InferenceContext): Ty {
    val binding =
        refExpr.path.reference?.resolve() as? MvBindingPat ?: return TyUnknown
    return binding.inferredTy(ctx)
}

private fun inferBorrowExprTy(borrowExpr: MvBorrowExpr, ctx: InferenceContext): Ty {
    val innerExpr = borrowExpr.expr ?: return TyUnknown
    val innerExprTy = inferExprTy(innerExpr, ctx)
    val mutabilities = RefPermissions.valueOf(borrowExpr.isMut)
    return TyReference(innerExprTy, mutabilities, ctx.msl)
}

private fun inferCallExprTy(
    callExpr: MvCallExpr,
    parentCtx: InferenceContext,
    expectedTy: Ty?
): Ty {
    val existingTy = parentCtx.callExprTypes[callExpr]
    if (existingTy != null) {
        return existingTy
    }

    val path = callExpr.path
    val funcItem = path.reference?.resolve() as? MvFunctionLike ?: return TyUnknown

    var funcTy = instantiateItemTy(funcItem, parentCtx) as? TyFunction ?: return TyUnknown
    val inferenceCtx = InferenceContext(parentCtx.msl)
    // find all types passed as explicit type parameters, create constraints with those
    if (path.typeArguments.isNotEmpty()) {
        if (path.typeArguments.size != funcTy.typeVars.size) return TyUnknown
        for ((typeVar, typeArg) in funcTy.typeVars.zip(path.typeArguments)) {
            val typeArgTy = inferTypeTy(typeArg.type, parentCtx)

            // check compat for abilities
            val compat = isCompatibleAbilities(typeVar, typeArgTy, path.isMsl())
            val isCompat = when (compat) {
                is Compat.AbilitiesMismatch -> {
                    parentCtx.typeErrors.add(
                        TypeError.AbilitiesMismatch(
                            typeArg,
                            typeArgTy,
                            compat.abilities
                        )
                    )
                    false
                }

                else -> true
            }
            inferenceCtx.addConstraint(typeVar, if (isCompat) typeArgTy else TyUnknown)
        }
    }
    // find all types of passed expressions, create constraints with those
    for ((i, argumentExpr) in callExpr.callArgumentExprs.withIndex()) {
        inferenceCtx.processConstraints()
        funcTy = inferenceCtx.resolveTy(funcTy) as TyFunction

        val paramTy = funcTy.paramTypes.getOrNull(i) ?: break
        val argumentExprTy = inferExprTy(argumentExpr, parentCtx, paramTy)
        inferenceCtx.addConstraint(paramTy, argumentExprTy)
    }
    if (expectedTy != null) {
        inferenceCtx.addConstraint(funcTy.retType, expectedTy)
    }

    inferenceCtx.processConstraints()
    funcTy = inferenceCtx.resolveTy(funcTy) as TyFunction

    parentCtx.cacheCallExprTy(callExpr, funcTy)
    return funcTy
}

private fun inferDotExprTy(dotExpr: MvDotExpr, parentCtx: InferenceContext): Ty {
    val objectTy = inferExprTy(dotExpr.expr, parentCtx)
    val structTy =
        when (objectTy) {
            is TyReference -> objectTy.referenced as? TyStruct
            is TyStruct -> objectTy
            else -> null
        } ?: return TyUnknown

    val inferenceCtx = InferenceContext(parentCtx.msl)
    for ((tyVar, tyArg) in structTy.typeVars.zip(structTy.typeArgs)) {
        inferenceCtx.addConstraint(tyVar, tyArg)
    }
    // solve constraints, return TyUnknown if cannot
    if (!inferenceCtx.processConstraints()) return TyUnknown

    val fieldName = dotExpr.structDotField.referenceName
    return inferenceCtx.resolveTy(structTy.fieldTy(fieldName))
}

fun inferStructLitExpr(
    litExpr: MvStructLitExpr,
    parentCtx: InferenceContext,
    expectedTy: Ty? = null
): Ty {
    val path = litExpr.path
    val structItem = path.maybeStruct ?: return TyUnknown
    val structTy = instantiateItemTy(structItem, parentCtx) as? TyStruct ?: return TyUnknown

    val inferenceCtx = InferenceContext(parentCtx.msl)
    // find all types passed as explicit type parameters, create constraints with those
    if (path.typeArguments.isNotEmpty()) {
        if (path.typeArguments.size != structTy.typeVars.size) return TyUnknown
        for ((typeVar, typeArg) in structTy.typeVars.zip(path.typeArguments)) {
            val typeArgTy = inferTypeTy(typeArg.type, parentCtx)

            // check compat for abilities
            val compat = isCompatibleAbilities(typeVar, typeArgTy, path.isMsl())
            val isCompat = when (compat) {
                is Compat.AbilitiesMismatch -> {
                    parentCtx.typeErrors.add(
                        TypeError.AbilitiesMismatch(
                            typeArg,
                            typeArgTy,
                            compat.abilities
                        )
                    )
                    false
                }

                else -> true
            }
            inferenceCtx.addConstraint(typeVar, if (isCompat) typeArgTy else TyUnknown)
        }
    }
    for (field in litExpr.fields) {
        val fieldName = field.referenceName
        val fieldTy = structTy.fieldTys[fieldName] ?: TyUnknown
        inferLitFieldInitExprTy(field, parentCtx, fieldTy)
    }
    if (expectedTy != null) {
        inferenceCtx.addConstraint(structTy, expectedTy)
    }
    inferenceCtx.processConstraints()

    parentCtx.resolveTyVarsFromContext(inferenceCtx)
    return inferenceCtx.resolveTy(structTy)
}

fun inferStructPatTy(structPat: MvStructPat, parentCtx: InferenceContext, expectedTy: Ty?): Ty {
    val path = structPat.path
    val struct = structPat.struct ?: return TyUnknown
    val structTy = instantiateItemTy(struct, parentCtx) as TyStruct

    val inferenceCtx = InferenceContext(parentCtx.msl)
    // find all types passed as explicit type parameters, create constraints with those
    if (path.typeArguments.isNotEmpty()) {
        if (path.typeArguments.size != structTy.typeVars.size) return TyUnknown
        for ((typeVar, typeArg) in structTy.typeVars.zip(path.typeArguments)) {
            val typeArgTy = inferTypeTy(typeArg.type, parentCtx)

            // check compat for abilities
            val compat = isCompatibleAbilities(typeVar, typeArgTy, path.isMsl())
            val isCompat = when (compat) {
                is Compat.AbilitiesMismatch -> {
                    parentCtx.typeErrors.add(
                        TypeError.AbilitiesMismatch(
                            typeArg,
                            typeArgTy,
                            compat.abilities
                        )
                    )
                    false
                }

                else -> true
            }
            inferenceCtx.addConstraint(typeVar, if (isCompat) typeArgTy else TyUnknown)
        }
    }
    if (expectedTy != null) {
        if (isCompatible(expectedTy, structTy)) {
            inferenceCtx.addConstraint(structTy, expectedTy)
        } else {
            parentCtx.typeErrors.add(TypeError.InvalidUnpacking(structPat, expectedTy))
        }
    }
    inferenceCtx.processConstraints()
    parentCtx.resolveTyVarsFromContext(inferenceCtx)
    return inferenceCtx.resolveTy(structTy)
}

fun inferVectorLitExpr(litExpr: MvVectorLitExpr, parentCtx: InferenceContext): Ty {
    var tyVector = TyVector(TyInfer.TyVar())
    val inferenceCtx = InferenceContext(litExpr.isMsl())
    val typeArgument = litExpr.typeArgument
    if (typeArgument != null) {
        val ty = inferTypeTy(typeArgument.type, parentCtx)
        inferenceCtx.addConstraint(tyVector.item, ty)
    }
    val exprs = litExpr.vectorLitItems.exprList
    for (expr in exprs) {
        inferenceCtx.processConstraints()
        tyVector = inferenceCtx.resolveTy(tyVector) as TyVector

        val exprTy = inferExprTy(expr, parentCtx, tyVector.item)
        inferenceCtx.addConstraint(tyVector.item, exprTy)
    }
    inferenceCtx.processConstraints()
    tyVector = inferenceCtx.resolveTy(tyVector) as TyVector
    return tyVector
}

fun inferLitFieldInitExprTy(litField: MvStructLitField, ctx: InferenceContext, expectedTy: Ty?): Ty {
    val initExpr = litField.expr
    return if (initExpr == null) {
        // find type of binding
        val binding =
            litField.reference.multiResolve().filterIsInstance<MvBindingPat>().firstOrNull()
                ?: return TyUnknown
        val bindingTy = binding.inferredTy(ctx)
        if (expectedTy != null) {
            if (!isCompatible(expectedTy, bindingTy, ctx.msl)) {
                ctx.typeErrors.add(TypeError.TypeMismatch(litField, expectedTy, bindingTy))
            } else {
                ctx.addConstraint(bindingTy, expectedTy)
            }
        }
        bindingTy
    } else {
        // find type of expression
        inferExprTy(initExpr, ctx, expectedTy)
    }
}

private fun inferBinaryExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
    return when (binaryExpr.binaryOp.op) {
        "<", ">", "<=", ">=", "==", "!=", "||", "&&", "==>", "<==>" -> inferBoolExprTy(binaryExpr, ctx)
        "+", "-", "*", "/", "%" -> inferBinaryArithmeticExprTy(binaryExpr, ctx)
        else -> TyUnknown
    }
}

private fun inferBinaryArithmeticExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty {
    val leftExpr = binaryExpr.left
    val rightExpr = binaryExpr.right

    var typeErrorEncountered = false
    val leftExprTy = inferExprTy(leftExpr, ctx)
    if (!leftExprTy.supportsArithmeticOp()) {
        ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(leftExpr, leftExprTy, "+"))
        typeErrorEncountered = true
    }
    if (rightExpr != null) {
        val rightExprTy = inferExprTy(rightExpr, ctx)
        if (!rightExprTy.supportsArithmeticOp()) {
            ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(rightExpr, rightExprTy, "+"))
            typeErrorEncountered = true
        }
        if (!typeErrorEncountered) {
            ctx.addConstraint(leftExprTy, rightExprTy)
        }
    }
    return if (typeErrorEncountered) TyUnknown else leftExprTy
}

private fun inferBoolExprTy(binaryExpr: MvBinaryExpr, ctx: InferenceContext): Ty { return TyBool }

private fun Ty.supportsArithmeticOp(): Boolean {
    return this is TyInteger
            || this is TyNum
            || this is TyTypeParameter
            || this is TyInfer
            || this is TyUnknown
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

private fun inferIfExprTy(ifExpr: MvIfExpr, ctx: InferenceContext, expectedTy: Ty?): Ty {
    val conditionExpr = ifExpr.condition?.expr
    if (conditionExpr != null) {
        inferExprTy(conditionExpr, ctx, TyBool)
    }

    val ifCodeBlock = ifExpr.codeBlock
    val ifInlineBlockExpr = ifExpr.inlineBlock?.expr
    val ifExprTy = when {
        ifCodeBlock != null -> {
            val blockCtx = ctx.childContext()
            inferCodeBlockTy(ifCodeBlock, blockCtx, expectedTy)
        }
        ifInlineBlockExpr != null -> {
            inferExprTy(ifInlineBlockExpr, ctx, expectedTy)
        }
        else -> return TyUnknown
    }

    val elseBlock = ifExpr.elseBlock ?: return TyUnknown
    val elseCodeBlock = elseBlock.codeBlock
    val elseInlineBlockExpr = elseBlock.inlineBlock?.expr
    val elseExprTy = when {
        elseCodeBlock != null -> {
            val blockCtx = ctx.childContext()
            inferCodeBlockTy(elseCodeBlock, blockCtx, expectedTy)
        }
        elseInlineBlockExpr != null -> {
            inferExprTy(elseInlineBlockExpr, ctx, expectedTy)
        }
        else -> return TyUnknown
    }

    return combineTys(ifExprTy, elseExprTy, ctx.msl)
}

private fun inferLoopExpr(expr: MvExpr, ctx: InferenceContext): Ty {
    if (expr is MvWhileExpr) {
        val conditionExpr = expr.condition?.expr
        if (conditionExpr != null) {
            inferExprTy(conditionExpr, ctx, TyBool)
        }
    }
    val (codeBlock, inlineBlockExpr) = when (expr) {
        is MvWhileExpr -> {
            val conditionExpr = expr.condition?.expr
            if (conditionExpr != null) {
                inferExprTy(conditionExpr, ctx, TyBool)
            }
            Pair(expr.codeBlock, expr.inlineBlock?.expr)
        }
        is MvLoopExpr -> Pair(expr.codeBlock, expr.inlineBlock?.expr)
        else -> error("unreachable")
    }
    when {
        codeBlock != null -> {
            val blockCtx = ctx.childContext()
            inferCodeBlockTy(codeBlock, blockCtx, TyUnit)
        }
        inlineBlockExpr != null -> inferExprTy(inlineBlockExpr, ctx, TyUnit)
    }
    return TyUnit
}
