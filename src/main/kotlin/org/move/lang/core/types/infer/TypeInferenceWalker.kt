package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*
import org.move.lang.utils.MoveDiagnostic

class TypeInferenceWalker(
    val ctx: FunctionInferenceContext
) {
    fun inferLiteralExprTy(literalExpr: MoveLiteralExpr): Ty {
        return when {
            literalExpr.boolLiteral != null -> TyBool
            literalExpr.addressLiteral != null
                    || literalExpr.bech32AddressLiteral != null
                    || literalExpr.polkadotAddressLiteral != null -> TyAddress
            literalExpr.integerLiteral != null || literalExpr.hexIntegerLiteral != null -> {
                val literal = (literalExpr.integerLiteral ?: literalExpr.hexIntegerLiteral)!!
                return TyInteger.fromSuffixedLiteral(literal) ?: TyInteger(TyInteger.DEFAULT_KIND)
            }
            literalExpr.byteStringLiteral != null -> TyByteString
            else -> TyUnknown
        }
    }

    fun inferCastExprTy(castExpr: MoveCastExpr): Ty {
        return inferMoveTypeTy(castExpr.type)
    }

    fun inferDotExprTy(dotExpr: MoveDotExpr): Ty {
        val receiverTy = inferExprTy(dotExpr.expr)
        val structTy = when (receiverTy) {
            is TyStruct -> receiverTy
            is TyReference -> receiverTy.innerTy() as? TyStruct ?: return TyUnknown
            else -> {
                // unsupported
                return TyUnknown
            }
        }
        val fieldName = dotExpr.structFieldRef.referenceName
        val fieldTy = structTy.item.structDef
            ?.fieldsMap?.get(fieldName)
            ?.typeAnnotation
            ?.type?.let { inferMoveTypeTy(it) }
        return fieldTy ?: TyUnknown
    }

    fun inferRefExprTy(expr: MoveRefExpr): Ty {
        val item = expr.path.reference?.resolve() ?: return TyUnknown
        return when (item) {
            is MoveBindingPat -> item.name?.let { this.ctx.bindings[it] } ?: TyUnknown
            is MoveNameIdentifierOwner -> instantiateItemTy(item)
            else -> TyUnknown
        }
    }

    fun inferCallExprTy(callExpr: MoveCallExpr): Ty {
        val solver = this.ctx.constraintSolver
        val path = callExpr.path
        val funcItem = path.reference?.resolve() as? MoveFunctionSignature ?: return TyUnknown
        val funcTy = instantiateItemTy(funcItem) as? TyFunction ?: return TyUnknown

        // find all types passed as explicit type parameters, create constraints with those
        if (path.typeArguments.isNotEmpty()) {
            if (path.typeArguments.size != funcTy.typeVars.size) return TyUnknown
            for ((typeVar, typeArgument) in funcTy.typeVars.zip(path.typeArguments)) {
                // TODO: resolve passedTy with current context
                val passedTy = inferMoveTypeTy(typeArgument.type)
                solver.registerConstraint(Constraint.Equate(typeVar, passedTy))
            }
        }
        // find all types of passed expressions, create constraints with those
        if (callExpr.arguments.isNotEmpty()) {
            for ((paramTy, argumentExpr) in funcTy.paramTypes.zip(callExpr.arguments)) {
                val argumentTy = this.inferExprTy(argumentExpr)
                solver.registerConstraint(Constraint.Equate(paramTy, argumentTy))
            }
        }
        // solve constraints
        solver.processConstraints()
        // see whether every arg is coerceable with those vars having those values
        // resolve return type with those vars
        return this.ctx.foldResolvingTyInfersFromCurrentContext(funcTy.retType)
    }

    fun inferBorrowExprTy(borrowExpr: MoveBorrowExpr): Ty {
        val innerTy = borrowExpr.expr?.let { inferExprTy(it) } ?: TyUnknown
        return TyReference(innerTy, Mutability.valueOf(borrowExpr.isMut))
    }

    fun inferIfExprTy(ifExpr: MoveIfExpr): Ty {
        ifExpr.condition?.expr?.let { inferExprTy(it, TyBool) }
        return TyUnknown
    }

    fun inferStructLiteralExprTy(structLiteralExpr: MoveStructLiteralExpr): Ty {
        return TyUnknown
    }

    fun inferExprTy(expr: MoveExpr, expected: Ty? = null): Ty {
        if (ctx.exprTypes.containsKey(expr)) {
            // TODO: add this
            // error("Trying to infer expression type twice")
            return ctx.exprTypes[expr]!!
        }

        val exprTy = when (expr) {
            is MoveRefExpr -> this.inferRefExprTy(expr)
            is MoveStructLiteralExpr -> this.inferStructLiteralExprTy(expr)
            is MoveCallExpr -> this.inferCallExprTy(expr)
            is MoveLiteralExpr -> this.inferLiteralExprTy(expr)
            is MoveCastExpr -> this.inferCastExprTy(expr)
            is MoveDotExpr -> this.inferDotExprTy(expr)
            is MoveBorrowExpr -> this.inferBorrowExprTy(expr)
            is MoveIfExpr -> this.inferIfExprTy(expr)
            is MoveParensExpr -> expr.expr?.let { this.inferExprTy(it) } ?: TyUnknown
            else -> TyUnknown
        }
        this.ctx.exprTypes[expr] = exprTy
        return exprTy
    }

    private fun instantiateItemTy(item: MoveNameIdentifierOwner): Ty {
        return when (item) {
            is MoveStructSignature -> TyStruct(item)
            is MoveFunctionSignature -> {
                val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

                fun findTypeVar(parameter: MoveTypeParameter): Ty {
                    return typeVars.find { it.origin?.parameter == parameter }!!
                }

                val paramTypes = mutableListOf<Ty>()
                for (param in item.parameters) {
                    val paramType = param.typeAnnotation?.type
                        ?.let { inferMoveTypeTy(it) }
                        ?.foldTyTypeParameterWith { findTypeVar(it.parameter) } ?: TyUnknown
                    paramTypes.add(paramType)
                }
                val retType = item.returnType?.type
                    ?.let { inferMoveTypeTy(it) }
                    ?.foldTyTypeParameterWith { findTypeVar(it.parameter) } ?: TyUnknown
                TyFunction(item, typeVars, paramTypes, retType)
            }
            else -> TyUnknown
        }
    }

    fun isCoerceableTypes(element: MoveElement, inferred: Ty, expected: Ty): Boolean {
        val coerceResult = tryCoerce(inferred, expected)
        return when (coerceResult) {
            CoerceResult.Ok -> true
            is CoerceResult.Mismatch -> {
                // TODO: ignore unsupported types
                reportTypeMismatch(element, inferred, expected)
                false
            }
        }
//        return isCoerceableResolved(element, inferred, expected)
    }

    // Another awful hack: check that inner expressions did not annotated as an error
    // to disallow annotation intersections. This should be done in a different way
    private fun reportTypeMismatch(element: MoveElement, expected: Ty, inferred: Ty) {
        if (ctx.diagnostics.all { !element.isAncestorOf(it.element) }) {
            addDiagnostic(MoveDiagnostic.TypeError(element, expected, inferred))
        }
    }

    fun addDiagnostic(diagnostic: MoveDiagnostic) {
        if (diagnostic.element.containingFile.isPhysical) {
            ctx.diagnostics.add(diagnostic)
        }
    }

    private fun isCoerceableResolved(element: MoveElement, inferred: Ty, expected: Ty): Boolean =
        when (val result = tryCoerce(inferred, expected)) {
            CoerceResult.Ok -> true
            is CoerceResult.Mismatch -> {
                // TODO: ignore unsupported types
                reportTypeMismatch(element, inferred, expected)
                false
            }
        }

    private fun tryCoerce(inferred: Ty, expected: Ty): CoerceResult {
        return when {
            inferred is TyReference && expected is TyReference &&
                    coerceMutability(inferred.mutability, expected.mutability) -> {
                coerceReference(inferred, expected)
            }
            else -> ctx.tryCoerceTypes(inferred, expected)
        }
    }

    private fun coerceMutability(from: Mutability, to: Mutability): Boolean =
        from == to || from.isMut && !to.isMut

    private fun coerceReference(inferred: TyReference, expected: TyReference): CoerceResult {
        return CoerceResult.Ok
    }
}
