package org.move.lang.core.types.infer

import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.mutable
import org.move.lang.core.psi.ext.typeArguments
import org.move.lang.core.types.ty.*

//fun tyFromBuiltinTypeLabel(label: String): Ty {
//    return when (label) {
//        in INTEGER_TYPE_IDENTIFIERS -> TyInteger.fromName(label)!!
//        "bool" -> TyBool
//        "address" -> TyAddress
//        "signer" -> TySigner
//        "vector" -> {
//            val itemTy = moveType.path.typeArguments
//                .firstOrNull()
//                ?.type
//                ?.let { inferMoveTypeTy(it) } ?: TyUnknown
//            return TyVector(itemTy)
//        }
//        else -> TyUnknown
//}

fun inferPrimitivePathType(moveType: MovePathType): Ty {
    val refName = moveType.path.referenceName ?: return TyUnknown
    return when (refName) {
        in INTEGER_TYPE_IDENTIFIERS -> TyInteger.fromName(refName)!!
        "bool" -> TyBool
        "address" -> TyAddress
        "signer" -> TySigner
        "vector" -> {
            val itemTy = moveType.path.typeArguments
                .firstOrNull()
                ?.type
                ?.let { inferMoveTypeTy(it) } ?: TyUnknown
            return TyVector(itemTy)
        }
        else -> TyUnknown
    }
}

fun inferMoveTypeTy(moveType: MoveType): Ty {
    return when (moveType) {
        is MovePathType -> {
            val referred = moveType.path.reference?.resolve()
            if (referred == null) return inferPrimitivePathType(moveType)

            return when (referred) {
                is MoveTypeParameter -> TyTypeParameter(referred)
                is MoveStructSignature -> {
                    val typeArgs = moveType.path.typeArguments.map { inferMoveTypeTy(it.type) }
                    TyStruct(referred, typeArgs)
                }
                else -> TyUnknown
            }
        }
        is MoveRefType -> {
            // TODO
            val mutability = Mutability.valueOf(moveType.mutable)
            val innerTypeRef = moveType.type ?: return TyReference(TyUnknown, mutability)
            val innerTy = inferMoveTypeTy(innerTypeRef)
            return TyReference(innerTy, mutability)
        }
        else -> TyUnknown
    }
}
