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

        is MvStructLitExpr -> inferStructLitExpr(expr, parentCtx, expectedTy)

        is MvDotExpr -> inferDotExprTy(expr, parentCtx)
        is MvDerefExpr -> inferDerefExprTy(expr, parentCtx)
        is MvLitExpr -> inferLitExprTy(expr, parentCtx)
        is MvTupleLitExpr -> inferTupleLitExprTy(expr, parentCtx)

        is MvMoveExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown
        is MvCopyExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown

        is MvCastExpr -> inferTypeTy(expr.type, parentCtx.msl)
        is MvParensExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown

        is MvPlusExpr -> inferBinaryExprTy(expr.exprList, parentCtx)
        is MvMinusExpr -> inferBinaryExprTy(expr.exprList, parentCtx)
        is MvMulExpr -> inferBinaryExprTy(expr.exprList, parentCtx)
        is MvDivExpr -> inferBinaryExprTy(expr.exprList, parentCtx)
        is MvModExpr -> inferBinaryExprTy(expr.exprList, parentCtx)

        is MvBangExpr -> TyBool
        is MvLessExpr -> TyBool
        is MvLessEqualsExpr -> TyBool
        is MvGreaterExpr -> TyBool
        is MvGreaterEqualsExpr -> TyBool
        is MvAndExpr -> TyBool
        is MvOrExpr -> TyBool

        is MvIfExpr -> inferIfExprTy(expr, parentCtx)

        else -> TyUnknown
    }
    if (exprTy is TyReference && expr.isMsl()) {
        exprTy = exprTy.innermostTy()
    }
    if (exprTy is TyInteger && expr.isMsl()) {
        exprTy = TyNum
    }
    if (expectedTy != null && isCompatible(expectedTy, exprTy)) {
        parentCtx.addConstraint(exprTy, expectedTy)
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
    val mutability = Mutability.valueOf(borrowExpr.isMut)
    return TyReference(innerExprTy, mutability, ctx.msl)
}

fun inferCallExprTy(
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
    val funcTy = instantiateItemTy(funcItem, parentCtx.msl) as? TyFunction ?: return TyUnknown

    val inferenceCtx = InferenceContext(parentCtx.msl)
    // find all types passed as explicit type parameters, create constraints with those
    if (path.typeArguments.isNotEmpty()) {
        if (path.typeArguments.size != funcTy.typeVars.size) return TyUnknown
        for ((typeVar, typeArg) in funcTy.typeVars.zip(path.typeArguments)) {
            val typeArgTy = inferTypeTy(typeArg.type, parentCtx.msl)

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

//            inferenceCtx.addConstraint(typeVar, passedTy)
        }
    }
    // find all types of passed expressions, create constraints with those
    if (callExpr.arguments.isNotEmpty()) {
        for ((paramTy, argumentExpr) in funcTy.paramTypes.zip(callExpr.arguments)) {
            val argumentExprTy = inferExprTy(argumentExpr, parentCtx)
            inferenceCtx.addConstraint(paramTy, argumentExprTy)
        }
    }
    if (expectedTy != null) {
        inferenceCtx.addConstraint(funcTy.retType, expectedTy)
    }
    // solve constraints
    val solvable = inferenceCtx.processConstraints()

    val resolvedFuncTy = inferenceCtx.resolveTy(funcTy) as TyFunction
    resolvedFuncTy.solvable = solvable

    parentCtx.resolveTyVarsFromContext(inferenceCtx)
    parentCtx.cacheCallExprTy(callExpr, resolvedFuncTy)
    return resolvedFuncTy
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
    return inferenceCtx.resolveTy(structTy.fieldTy(fieldName, parentCtx.msl))
}

fun inferStructLitExpr(
    litExpr: MvStructLitExpr,
    parentCtx: InferenceContext,
    expectedTy: Ty? = null
): Ty {
    val path = litExpr.path
    val structItem = path.maybeStruct ?: return TyUnknown
    val structTy = instantiateItemTy(structItem, parentCtx.msl) as? TyStruct ?: return TyUnknown

    val inferenceCtx = InferenceContext(parentCtx.msl)
    // find all types passed as explicit type parameters, create constraints with those
    if (path.typeArguments.isNotEmpty()) {
        if (path.typeArguments.size != structTy.typeVars.size) return TyUnknown
        for ((typeVar, typeArg) in structTy.typeVars.zip(path.typeArguments)) {
            val typeArgTy = inferTypeTy(typeArg.type, parentCtx.msl)

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
        val fieldInitExprTy = inferLitFieldInitExprTy(field, parentCtx)
        inferenceCtx.addConstraint(fieldTy, fieldInitExprTy)
    }
    if (expectedTy != null) {
        inferenceCtx.addConstraint(structTy, expectedTy)
    }
    inferenceCtx.processConstraints()

    parentCtx.resolveTyVarsFromContext(inferenceCtx)
    return inferenceCtx.resolveTy(structTy)
}

fun inferLitFieldInitExprTy(litField: MvStructLitField, ctx: InferenceContext): Ty {
    val initExpr = litField.expr
    return if (initExpr == null) {
        // find type of binding
        val binding =
            litField.reference.multiResolve().filterIsInstance<MvBindingPat>().firstOrNull()
                ?: return TyUnknown
        binding.inferredTy(ctx)
    } else {
        // find type of expression
        inferExprTy(initExpr, ctx)
    }
}

private fun inferBinaryExprTy(exprList: List<MvExpr>, ctx: InferenceContext): Ty {
    val leftExpr = exprList.getOrNull(0) ?: return TyUnknown
    val rightExpr = exprList.getOrNull(1)

    var typeErrorEncountered = false
    val leftExprTy = inferExprTy(leftExpr, ctx)
    if (!leftExprTy.supportsBinaryOp()) {
        ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(leftExpr, leftExprTy, "+"))
        typeErrorEncountered = true
    }
    if (rightExpr != null) {
        val rightExprTy = inferExprTy(rightExpr, ctx)
        if (!rightExprTy.supportsBinaryOp()) {
            ctx.typeErrors.add(TypeError.UnsupportedBinaryOp(rightExpr, rightExprTy, "+"))
            typeErrorEncountered = true
        }
        if (!typeErrorEncountered) {
            ctx.addConstraint(leftExprTy, rightExprTy)
        }
    }
    return if (typeErrorEncountered) TyUnknown else leftExprTy
}

private fun Ty.supportsBinaryOp(): Boolean {
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

private fun inferIfExprTy(ifExpr: MvIfExpr, ctx: InferenceContext): Ty {
    val ifTy = ifExpr.returningExpr?.let { inferExprTy(it, ctx) } ?: return TyUnknown
    val elseTy = ifExpr.elseExpr?.let { inferExprTy(it, ctx) } ?: return TyUnknown
    return combineTys(ifTy, elseTy)
}
