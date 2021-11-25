package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.hexIntegerLiteral
import org.move.lang.core.types.ty.*

fun instantiateItemTy(item: MoveNameIdentifierOwner): Ty {
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

fun isCompatible(ty1: Ty, ty2: Ty): Boolean {
    if (ty1 is TyUnknown || ty2 is TyUnknown) return true
    return true
}

fun Ty.compatibleWith(ty: Ty): Boolean {
    return isCompatible(this, ty)
}

class InferenceContext {
    private val unificationTable = UnificationTable<TyInfer.TyVar, Ty>()
    private val solver = ConstraintSolver(unificationTable)

    fun registerConstraint(constraint: Constraint) {
        solver.registerConstraint(constraint)
    }

    fun processConstraints() {
        solver.processConstraints()
    }

    fun resolveTy(ty: Ty): Ty {
        return ty.foldTyInferWith(this::resolveTyInferFromContext)
    }

    private fun resolveTyInferFromContext(ty: Ty): Ty {
        if (ty !is TyInfer) return ty
        return when (ty) {
            is TyInfer.TyVar -> unificationTable.findValue(ty)?.let(this::resolveTyInferFromContext) ?: ty
        }
    }


}
