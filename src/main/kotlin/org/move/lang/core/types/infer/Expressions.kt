package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*

fun inferExprTy(expr: MvExpr, ctx: InferenceContext): Ty {
    if (ctx.exprTypes.containsKey(expr)) return ctx.exprTypes[expr]!!

    val exprTy = when (expr) {
        is MvRefExpr -> inferRefExprTy(expr, ctx)
        is MvBorrowExpr -> inferBorrowExprTy(expr, ctx)
        is MvCallExpr -> inferCallExprTy(expr, ctx)
        is MvDotExpr -> inferDotExprTy(expr, ctx)
        is MvStructLitExpr -> inferStructLitExpr(expr, ctx)
        is MvDerefExpr -> inferDerefExprTy(expr, ctx)
        is MvLitExpr -> inferLitExprTy(expr)

        is MvMoveExpr -> expr.expr?.let { inferExprTy(it, ctx) } ?: TyUnknown
        is MvCopyExpr -> expr.expr?.let { inferExprTy(it, ctx) } ?: TyUnknown

        is MvCastExpr -> inferMvTypeTy(expr.type)
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

        is MvIfExpr -> inferIfExprTy(expr)
        else -> TyUnknown
    }
    ctx.cacheExprTy(expr, exprTy)
    return exprTy
}

private fun inferRefExprTy(refExpr: MvRefExpr, ctx: InferenceContext): Ty {
    val binding =
        refExpr.path.reference?.resolve() as? MvBindingPat ?: return TyUnknown
    return binding.inferBindingPatTy()
}

private fun inferBorrowExprTy(borrowExpr: MvBorrowExpr, ctx: InferenceContext): Ty {
    val innerExpr = borrowExpr.expr ?: return TyUnknown
    val innerExprTy = inferExprTy(innerExpr, ctx)
    val mutability = Mutability.valueOf(borrowExpr.isMut)
    return TyReference(innerExprTy, mutability)
}

private fun inferCallExprTy(callExpr: MvCallExpr, ctx: InferenceContext): Ty {
    val path = callExpr.path
    val funcItem = path.reference?.resolve() as? MvFunctionSignature ?: return TyUnknown
    val funcTy = instantiateItemTy(funcItem) as? TyFunction ?: return TyUnknown

    val inference = InferenceContext()
    // find all types passed as explicit type parameters, create constraints with those
    if (path.typeArguments.isNotEmpty()) {
        if (path.typeArguments.size != funcTy.typeVars.size) return TyUnknown
        for ((typeVar, typeArgument) in funcTy.typeVars.zip(path.typeArguments)) {
            val passedTy = inferMvTypeTy(typeArgument.type)
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
    // solve constraints, return TyUnknown if cannot
    if (!inference.processConstraints()) return TyUnknown

    // see whether every arg is coerceable with those vars having those values
    // resolve return type with those vars
    return inference.resolveTy(funcTy.retType)
}

private fun inferDotExprTy(dotExpr: MvDotExpr, ctx: InferenceContext): Ty {
    val objectTy = inferExprTy(dotExpr.expr, ctx)
    val structTy =
        when (objectTy) {
            is TyReference -> objectTy.referenced as? TyStruct
            is TyStruct -> objectTy
            else -> null
        } ?: return TyUnknown

    val inference = InferenceContext()
    for ((tyVar, tyArg) in structTy.typeVars.zip(structTy.typeArguments)) {
        inference.registerConstraint(Constraint.Equate(tyVar, tyArg))
    }
    // solve constraints, return TyUnknown if cannot
    if (!inference.processConstraints()) return TyUnknown

    val fieldName = dotExpr.structFieldRef.referenceName
    return inference.resolveTy(structTy.fieldTy(fieldName))
}

private fun inferStructLitExpr(litExpr: MvStructLitExpr, ctx: InferenceContext): Ty {
    val structItem = litExpr.path.maybeStructSignature ?: return TyUnknown
    val structTypeVars = structItem.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

    val inference = InferenceContext()
    // TODO: combine it with TyStruct constructor
    val typeArgs = litExpr.path.typeArguments
    if (typeArgs.isNotEmpty()) {
        if (typeArgs.size < structItem.typeParameters.size) return TyUnknown
        for ((tyVar, typeArg) in structTypeVars.zip(typeArgs)) {
            inference.registerConstraint(Constraint.Equate(tyVar, typeArg.type.inferTypeTy()))
        }
    }
    for (field in litExpr.providedFields) {
        val fieldName = field.referenceName
        val declaredFieldTy = structItem.structDef
            .fieldsMap[fieldName]
            ?.declaredTy
            ?.foldTyTypeParameterWith { param -> structTypeVars.find { it.origin?.parameter == param.parameter }!! }
            ?: TyUnknown
        val fieldExprTy = field.inferAssignedExprTy(ctx)
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

private fun inferLitExprTy(litExpr: MvLitExpr): Ty {
    return when {
        litExpr.boolLiteral != null -> TyBool
        litExpr.addressLiteral != null
                || litExpr.bech32AddressLiteral != null
                || litExpr.polkadotAddressLiteral != null -> TyAddress
        litExpr.integerLiteral != null || litExpr.hexIntegerLiteral != null -> {
            val literal = (litExpr.integerLiteral ?: litExpr.hexIntegerLiteral)!!
            return TyInteger.fromSuffixedLiteral(literal) ?: TyInteger(TyInteger.DEFAULT_KIND)
        }
        litExpr.byteStringLiteral != null -> TyByteString
        else -> TyUnknown
    }
}

private fun inferIfExprTy(ifExpr: MvIfExpr): Ty {
    val ifTy = ifExpr.returningExpr?.inferExprTy() ?: return TyUnknown
    val elseTy = ifExpr.elseExpr?.inferExprTy() ?: return TyUnknown
    if (!isCompatible(ifTy, elseTy)) return TyUnknown
    return ifTy
}
