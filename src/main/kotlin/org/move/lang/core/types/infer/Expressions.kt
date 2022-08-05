package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*

fun inferExprTy(expr: MvExpr, parentCtx: InferenceContext): Ty {
    val existingTy = parentCtx.exprTypes[expr]
    if (existingTy != null) {
        return existingTy
    }

    var exprTy = when (expr) {
        is MvRefExpr -> inferRefExprTy(expr, parentCtx)
        is MvBorrowExpr -> inferBorrowExprTy(expr, parentCtx)
        is MvCallExpr -> {
            val funcTy = inferCallExprTy(expr, parentCtx, null) as? TyFunction
            if (funcTy == null || !funcTy.solvable) {
                TyUnknown
            } else {
                funcTy.retType
            }
        }
        is MvDotExpr -> inferDotExprTy(expr, parentCtx)
        is MvStructLitExpr -> inferStructLitExpr(expr, parentCtx)
        is MvDerefExpr -> inferDerefExprTy(expr, parentCtx)
        is MvLitExpr -> inferLitExprTy(expr, parentCtx)
        is MvTupleLitExpr -> inferTupleLitExprTy(expr, parentCtx)

        is MvMoveExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown
        is MvCopyExpr -> expr.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown

        is MvCastExpr -> inferMvTypeTy(expr.type, parentCtx.msl)
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
        for ((typeVar, typeArgument) in funcTy.typeVars.zip(path.typeArguments)) {
            val passedTy = inferMvTypeTy(typeArgument.type, parentCtx.msl)
            inferenceCtx.registerConstraint(EqualityConstraint(typeVar, passedTy))
        }
    }
    // find all types of passed expressions, create constraints with those
    if (callExpr.arguments.isNotEmpty()) {
        for ((paramTy, argumentExpr) in funcTy.paramTypes.zip(callExpr.arguments)) {
            val argumentExprTy = inferExprTy(argumentExpr, parentCtx)
            inferenceCtx.registerConstraint(EqualityConstraint(paramTy, argumentExprTy))
        }
    }
    if (expectedTy != null) {
        inferenceCtx.registerConstraint(EqualityConstraint(funcTy.retType, expectedTy))
    }
    // solve constraints
    val solvable = inferenceCtx.processConstraints()

    val resolvedFuncTy = inferenceCtx.resolveTy(funcTy) as TyFunction
    resolvedFuncTy.solvable = solvable
    // if there's any unsolved TyInfer left, then unsolvable
//    resolvedFuncTy.foldTyInferWith { TyUnknown }
//    resolvedFuncTy.foldTyInferWith { resolvedFuncTy.solvable = false; it }

    parentCtx.exprTypes = inferenceCtx.resolveTyMap(parentCtx.exprTypes)
    parentCtx.bindingTypes = inferenceCtx.resolveTyMap(parentCtx.bindingTypes)

    parentCtx.cacheCallExprTy(callExpr, resolvedFuncTy)
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
        inference.registerConstraint(EqualityConstraint(tyVar, tyArg))
    }
    // solve constraints, return TyUnknown if cannot
    if (!inference.processConstraints()) return TyUnknown

    val fieldName = dotExpr.structDotField.referenceName
    return inference.resolveTy(structTy.fieldTy(fieldName, ctx.msl))
}

fun inferStructLitExpr(litExpr: MvStructLitExpr, ctx: InferenceContext): Ty {
    val structItem = litExpr.path.maybeStruct ?: return TyUnknown
    val structTypeVars = structItem.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

    val inference = InferenceContext(ctx.msl)
    // TODO: combine it with TyStruct constructor
    val typeArgs = litExpr.path.typeArguments
    if (typeArgs.isNotEmpty()) {
        if (typeArgs.size < structItem.typeParameters.size) return TyUnknown
        for ((tyVar, typeArg) in structTypeVars.zip(typeArgs)) {
            inference.registerConstraint(EqualityConstraint(tyVar, typeArg.type.ty()))
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
        inference.registerConstraint(EqualityConstraint(declaredFieldTy, fieldExprTy))
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
