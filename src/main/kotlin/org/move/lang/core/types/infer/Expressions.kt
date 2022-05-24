package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*

fun inferExprTy(expr: MvExpr, ctx: InferenceContext): Ty {
    val existingTy = ctx.exprTypes[expr]
    if (existingTy != null) {
        return existingTy
    }

    var exprTy = when (expr) {
        is MvRefExpr -> inferRefExprTy(expr, ctx)
        is MvBorrowExpr -> inferBorrowExprTy(expr, ctx)
        is MvCallExpr -> {
            val funcTy = inferCallExprTy(expr, ctx) as? TyFunction
            if (funcTy == null || !funcTy.solvable) {
                TyUnknown
            } else {
                funcTy.retType
            }
        }
        is MvDotExpr -> inferDotExprTy(expr, ctx)
        is MvStructLitExpr -> inferStructLitExpr(expr, ctx)
        is MvDerefExpr -> inferDerefExprTy(expr, ctx)
        is MvLitExpr -> inferLitExprTy(expr, ctx)
        is MvTupleLitExpr -> inferTupleLitExprTy(expr, ctx)

        is MvMoveExpr -> expr.expr?.let { inferExprTy(it, ctx) } ?: TyUnknown
        is MvCopyExpr -> expr.expr?.let { inferExprTy(it, ctx) } ?: TyUnknown

        is MvCastExpr -> inferMvTypeTy(expr.type, ctx.msl)
        is MvParensExpr -> expr.expr?.let { inferExprTy(it, ctx) } ?: TyUnknown

        is MvPlusExpr -> inferBinaryExprTy(expr.exprList, ctx)
        is MvMinusExpr -> inferBinaryExprTy(expr.exprList, ctx)
        is MvMulExpr -> inferBinaryExprTy(expr.exprList, ctx)
        is MvDivExpr -> inferBinaryExprTy(expr.exprList, ctx)
        is MvModExpr -> inferBinaryExprTy(expr.exprList, ctx)

        is MvBangExpr -> TyBool
        is MvLessExpr -> TyBool
        is MvLessEqualsExpr -> TyBool
        is MvGreaterExpr -> TyBool
        is MvGreaterEqualsExpr -> TyBool
        is MvAndExpr -> TyBool
        is MvOrExpr -> TyBool

        is MvIfExpr -> inferIfExprTy(expr, ctx)

        else -> TyUnknown
    }
    if (exprTy is TyReference && expr.isMsl()) {
        exprTy = exprTy.innermostTy()
    }
    if (exprTy is TyInteger && expr.isMsl()) {
        exprTy = TyNum
    }
    ctx.cacheExprTy(expr, exprTy)
    return exprTy
}

private fun inferRefExprTy(refExpr: MvRefExpr, ctx: InferenceContext): Ty {
    val binding =
        refExpr.path.reference?.resolve() as? MvBindingPat ?: return TyUnknown
    return binding.cachedTy(ctx)
}

private fun inferBorrowExprTy(borrowExpr: MvBorrowExpr, ctx: InferenceContext): Ty {
    val innerExpr = borrowExpr.expr ?: return TyUnknown
    val innerExprTy = inferExprTy(innerExpr, ctx)
    val mutability = Mutability.valueOf(borrowExpr.isMut)
    return TyReference(innerExprTy, mutability, ctx.msl)
}

fun inferCallExprTy(callExpr: MvCallExpr, ctx: InferenceContext): Ty {
    val existingTy = ctx.callExprTypes[callExpr]
    if (existingTy != null) {
        return existingTy
    }

    val path = callExpr.path
    val funcItem = path.reference?.resolve() as? MvFunctionLike ?: return TyUnknown
    val funcTy = instantiateItemTy(funcItem, ctx.msl) as? TyFunction ?: return TyUnknown

    val inference = InferenceContext(ctx.msl)
    // find all types passed as explicit type parameters, create constraints with those
    if (path.typeArguments.isNotEmpty()) {
        if (path.typeArguments.size != funcTy.typeVars.size) return TyUnknown
        for ((typeVar, typeArgument) in funcTy.typeVars.zip(path.typeArguments)) {
            val passedTy = inferMvTypeTy(typeArgument.type, ctx.msl)
            inference.registerConstraint(Constraint.Equate(typeVar, passedTy))
        }
    }
    // find all types of passed expressions, create constraints with those
    if (callExpr.arguments.isNotEmpty()) {
        for ((paramTy, argumentExpr) in funcTy.paramTypes.zip(callExpr.arguments)) {
            val argumentTy = inferExprTy(argumentExpr, ctx)
            inference.registerConstraint(Constraint.Equate(paramTy, argumentTy))
        }
    }
//    if (expectedTy != null) {
//        inference.registerConstraint(Constraint.Equate(funcTy.retType, expectedTy))
//    }
    // solve constraints
    val solvable = inference.processConstraints()

    // see whether every arg is coerceable with those vars having those values
    val resolvedFuncTy = inference.resolveTy(funcTy) as TyFunction
    resolvedFuncTy.solvable = solvable
    ctx.cacheCallExprTy(callExpr, resolvedFuncTy)

    return resolvedFuncTy
}

private fun inferDotExprTy(dotExpr: MvDotExpr, ctx: InferenceContext): Ty {
    val objectTy = inferExprTy(dotExpr.expr, ctx)
    val structTy =
        when (objectTy) {
            is TyReference -> objectTy.referenced as? TyStruct
            is TyStruct -> objectTy
            else -> null
        } ?: return TyUnknown

    val inference = InferenceContext(ctx.msl)
    for ((tyVar, tyArg) in structTy.typeVars.zip(structTy.typeArgs)) {
        inference.registerConstraint(Constraint.Equate(tyVar, tyArg))
    }
    // solve constraints, return TyUnknown if cannot
    if (!inference.processConstraints()) return TyUnknown

    val fieldName = dotExpr.structDotField.referenceName
    return inference.resolveTy(structTy.fieldTy(fieldName, ctx.msl))
}

private fun inferStructLitExpr(litExpr: MvStructLitExpr, ctx: InferenceContext): Ty {
    val structItem = litExpr.path.maybeStruct ?: return TyUnknown
    val structTypeVars = structItem.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

    val inference = InferenceContext(ctx.msl)
    // TODO: combine it with TyStruct constructor
    val typeArgs = litExpr.path.typeArguments
    if (typeArgs.isNotEmpty()) {
        if (typeArgs.size < structItem.typeParameters.size) return TyUnknown
        for ((tyVar, typeArg) in structTypeVars.zip(typeArgs)) {
            inference.registerConstraint(Constraint.Equate(tyVar, typeArg.type.ty()))
        }
    }
    for (field in litExpr.fields) {
        val fieldName = field.referenceName
        val declaredFieldTy = structItem
            .fieldsMap[fieldName]
            ?.declaredTy(ctx.msl)
            ?.foldTyTypeParameterWith { param -> structTypeVars.find { it.origin?.parameter == param.parameter }!! }
            ?: TyUnknown
        val fieldExprTy = field.inferInitExprTy(ctx)
        inference.registerConstraint(Constraint.Equate(declaredFieldTy, fieldExprTy))
    }
    // solve constraints, return TyUnknown if cannot
    if (!inference.processConstraints()) return TyUnknown

    val structTy = TyStruct(structItem, structTypeVars)
    return inference.resolveTy(structTy)
}

private fun inferBinaryExprTy(exprList: List<MvExpr>, ctx: InferenceContext): Ty {
    for ((i, expr) in exprList.withIndex()) {
        val exprTy = inferExprTy(expr, ctx)
        if (exprTy is TyInteger && exprTy.kind == TyInteger.DEFAULT_KIND) {
            if (i == exprList.lastIndex) return exprTy
            continue
        }
        return exprTy
    }
    return TyUnknown
}

private fun inferDerefExprTy(derefExpr: MvDerefExpr, ctx: InferenceContext): Ty {
    val exprTy =
        derefExpr.expr?.let { inferExprTy(it, ctx) }
    return (exprTy as? TyReference)?.referenced ?: TyUnknown
}

private fun inferTupleLitExprTy(tupleExpr: MvTupleLitExpr, ctx: InferenceContext): Ty {
    val types = tupleExpr.exprList.map { it.inferExprTy(ctx) }
    return TyTuple(types)
}

private fun inferLitExprTy(litExpr: MvLitExpr, ctx: InferenceContext): Ty {
    return when {
        litExpr.boolLiteral != null -> TyBool
        litExpr.addressLit != null -> TyAddress
        litExpr.integerLiteral != null || litExpr.hexIntegerLiteral != null -> {
            if (ctx.msl) return TyNum
            val literal = (litExpr.integerLiteral ?: litExpr.hexIntegerLiteral)!!
            return TyInteger.fromSuffixedLiteral(literal) ?: TyInteger(TyInteger.DEFAULT_KIND)
        }
        litExpr.byteStringLiteral != null -> TyByteString(ctx.msl)
        litExpr.hexStringLiteral != null -> TyHexString(ctx.msl)
        else -> TyUnknown
    }
}

private fun inferIfExprTy(ifExpr: MvIfExpr, ctx: InferenceContext): Ty {
    val ifTy = ifExpr.returningExpr?.inferExprTy(ctx) ?: return TyUnknown
    val elseTy = ifExpr.elseExpr?.inferExprTy(ctx) ?: return TyUnknown
    return combineTys(ifTy, elseTy)
}
