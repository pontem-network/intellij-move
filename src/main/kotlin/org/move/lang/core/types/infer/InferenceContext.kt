package org.move.lang.core.types.infer

import com.intellij.psi.util.parentOfType
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.fqName
import org.move.lang.core.psi.ext.hexIntegerLiteral
import org.move.lang.core.psi.impl.MoveFunctionDefImpl
import org.move.lang.core.types.ty.*

val MoveExpr.outerFunction
    get() = (parentOfType<MoveFunctionDef>(true) as? MoveFunctionDefImpl)!!

//fun prepareFunctionInferenceContext(function: MoveFunctionDef) {
//    val inference = InferenceContext()
//    val statements = function.codeBlock?.statementList.orEmpty()
//    for (statement in statements) {
//        when (statement) {
//            is MoveLetStatement -> null
//            is MoveExprStatement -> null
//        }
//    }
//}

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
            val returnMoveType = item.returnType?.type
            val retType: Ty
            if (returnMoveType == null) {
                retType = TyUnit
            } else {
                retType = inferMoveTypeTy(returnMoveType)
                    .foldTyTypeParameterWith { findTypeVar(it.parameter) }
            }
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


//sealed class TypesCompatibility {
//    object Ok : TypesCompatibility()
//    class Mismatch(val ty1: Ty, ty2: Ty) : TypesCompatibility()
//    class AbilitiesMismatch(val ty1: Ty, ty2: Ty) : TypesCompatibility()
//
//    val isOk: Boolean get() = this is Ok
//}

fun isCompatibleMutability(from: Mutability, to: Mutability): Boolean =
    from == to || from.isMut && !to.isMut

fun isCompatibleReferences(expectedTy: TyReference, inferredTy: TyReference): Boolean {
    return isCompatible(expectedTy.referenced, inferredTy.referenced)
}

fun isCompatibleStructs(expectedTy: TyStruct, inferredTy: TyStruct): Boolean {
    return expectedTy.item.fqName == inferredTy.item.fqName
            && expectedTy.typeArguments.size == inferredTy.typeArguments.size
            && expectedTy.typeArguments.zip(inferredTy.typeArguments)
        .all { isCompatible(it.first, it.second) }
}

fun isCompatibleIntegers(expectedTy: TyInteger, inferredTy: TyInteger): Boolean {
    return expectedTy.kind == TyInteger.DEFAULT_KIND
            || inferredTy.kind == TyInteger.DEFAULT_KIND
            || expectedTy.kind == inferredTy.kind
}

fun isCompatible(expectedTy: Ty, inferredTy: Ty): Boolean {
    return when {
        expectedTy is TyUnknown || inferredTy is TyUnknown -> true
        expectedTy is TyInfer.TyVar || inferredTy is TyInfer.TyVar -> {
            // check abilities
            true
        }
        expectedTy is TyTypeParameter || inferredTy is TyTypeParameter -> {
            // check abilities
            true
        }
        expectedTy is TyInteger && inferredTy is TyInteger -> isCompatibleIntegers(expectedTy, inferredTy)
        expectedTy is TyPrimitive && inferredTy is TyPrimitive
                && expectedTy.name == inferredTy.name -> true
        expectedTy is TyVector && inferredTy is TyVector
                && isCompatible(expectedTy.item, inferredTy.item) -> true

        expectedTy is TyReference && inferredTy is TyReference
                && isCompatibleMutability(inferredTy.mutability, expectedTy.mutability) ->
            isCompatibleReferences(expectedTy, inferredTy)

        expectedTy is TyStruct && inferredTy is TyStruct -> isCompatibleStructs(expectedTy, inferredTy)
        else -> false
    }
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
