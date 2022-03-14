package org.move.lang.core.types.infer

import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_INTEGER_TYPE_IDENTIFIERS
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

fun inferPrimitiveTypeTy(moveType: MvPathType, msl: Boolean): Ty {
    val refName = moveType.path.referenceName ?: return TyUnknown
    if (msl && refName in SPEC_INTEGER_TYPE_IDENTIFIERS) return TyInteger.fromName("num")
    return when (refName) {
        in INTEGER_TYPE_IDENTIFIERS -> TyInteger.fromName(refName)
        "bool" -> TyBool
        "address" -> TyAddress
        "signer" -> TySigner
        "vector" -> {
            val itemTy = moveType.path.typeArguments
                .firstOrNull()
                ?.type
                ?.let { inferMvTypeTy(it, msl) } ?: TyUnknown
            return TyVector(itemTy)
        }
        else -> TyUnknown
    }
}

fun inferMvTypeTy(moveType: MvType, msl: Boolean): Ty {
    return when (moveType) {
        is MvPathType -> {
            val referred = moveType.path.reference?.resolve()
            if (referred == null) return inferPrimitiveTypeTy(moveType, msl)
            when (referred) {
                is MvTypeParameter -> TyTypeParameter(referred)
                is MvStruct -> {
                    val typeArgs = moveType.path.typeArguments.map { inferMvTypeTy(it.type, msl) }
                    TyStruct(referred, typeArgs)
                }
                else -> TyUnknown
            }
        }
        is MvRefType -> {
            val mutability = Mutability.valueOf(moveType.mutable)
            val innerTypeRef = moveType.type ?: return TyReference(TyUnknown, mutability, msl)
            val innerTy = inferMvTypeTy(innerTypeRef, msl)
            TyReference(innerTy, mutability, msl)
        }
        is MvTupleType -> {
            val innerTypes = moveType.typeList.map { inferMvTypeTy(it, msl) }
            TyTuple(innerTypes)
        }
        is MvUnitType -> TyUnit
        else -> TyUnknown
    }
}
