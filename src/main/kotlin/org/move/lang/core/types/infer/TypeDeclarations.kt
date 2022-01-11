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
//                ?.let { inferMvTypeTy(it) } ?: TyUnknown
//            return TyVector(itemTy)
//        }
//        else -> TyUnknown
//}

fun inferPrimitivePathType(moveType: MvPathType): Ty {
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
                ?.let { inferMvTypeTy(it) } ?: TyUnknown
            return TyVector(itemTy)
        }
        else -> TyUnknown
    }
}

fun inferMvTypeTy(moveType: MvType): Ty {
    return when (moveType) {
        is MvPathType -> {
            val referred = moveType.path.reference?.resolve()
            if (referred == null) return inferPrimitivePathType(moveType)
            when (referred) {
                is MvTypeParameter -> TyTypeParameter(referred)
                is MvStruct_ -> {
                    val typeArgs = moveType.path.typeArguments.map { inferMvTypeTy(it.type) }
                    TyStruct(referred, typeArgs)
                }
                else -> TyUnknown
            }
        }
        is MvRefType -> {
            val mutability = Mutability.valueOf(moveType.mutable)
            val innerTypeRef = moveType.type ?: return TyReference(TyUnknown, mutability)
            val innerTy = inferMvTypeTy(innerTypeRef)
            TyReference(innerTy, mutability)
        }
        is MvTupleType -> {
            val innerTypes = moveType.typeList.map { inferMvTypeTy(it) }
            TyTuple(innerTypes)
        }
        is MvUnitType -> TyUnit
        else -> TyUnknown
    }
}
