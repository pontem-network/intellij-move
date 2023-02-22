package org.move.lang.core.types.infer

import com.intellij.psi.util.parentOfType
import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_INTEGER_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.mutable
import org.move.lang.core.psi.ext.typeArguments
import org.move.lang.core.types.ty.*

fun inferItemTypeTy(moveType: MvType, itemContext: ItemContext): Ty {
    val ty = when (moveType) {
        is MvPathType -> run {
            val namedItem = moveType.path.reference?.resolve()
                    ?: return@run inferItemBuiltinTypeTy(moveType, itemContext)
            when (namedItem) {
                is MvTypeParameter -> TyTypeParameter(namedItem)
                is MvStruct -> {
                    // check that it's not a recursive type
                    val parentStruct = moveType.parentOfType<MvStruct>()
                    if (parentStruct != null && namedItem == parentStruct) {
                        itemContext.typeErrors.add(TypeError.CircularType(moveType, parentStruct))
                        return TyUnknown
                    }

                    val rawStructTy = itemContext.getStructItemTy(namedItem) ?: return TyUnknown

                    val ctx = InferenceContext(itemContext.msl, itemContext)
                    if (rawStructTy.typeVars.isNotEmpty()) {
                        val typeArgs = moveType.path.typeArguments.map { inferItemTypeTy(it.type, itemContext) }
                        for ((tyVar, tyArg) in rawStructTy.typeVars.zip(typeArgs)) {
                            ctx.addConstraint(tyVar, tyArg)
                        }
                        ctx.processConstraints()
                    }
                    ctx.resolveTy(rawStructTy)
                }
                else -> TyUnknown
            }
        }
        is MvRefType -> run {
            val mutabilities = RefPermissions.valueOf(moveType.mutable)
            val refInnerType = moveType.type
                ?: return@run TyReference(TyUnknown, mutabilities, itemContext.msl)
            val innerTy = inferItemTypeTy(refInnerType, itemContext)
            TyReference(innerTy, mutabilities, itemContext.msl)
        }
        is MvTupleType -> {
            val innerTypes = moveType.typeList.map { inferItemTypeTy(it, itemContext) }
            TyTuple(innerTypes)
        }
        is MvUnitType -> TyUnit
        else -> TyUnknown
    }
    return ty
}

fun inferItemBuiltinTypeTy(pathType: MvPathType, itemContext: ItemContext): Ty {
    val refName = pathType.path.referenceName ?: return TyUnknown
    if (itemContext.msl && refName in SPEC_INTEGER_TYPE_IDENTIFIERS) return TyInteger.fromName("num")

    val ty = when (refName) {
        in INTEGER_TYPE_IDENTIFIERS -> TyInteger.fromName(refName)
        "bool" -> TyBool
        "address" -> TyAddress
        "signer" -> TySigner
        "vector" -> {
            val itemTy = pathType.path.typeArguments
                .firstOrNull()
                ?.type
                ?.let { inferItemTypeTy(it, itemContext) } ?: TyUnknown
            return TyVector(itemTy)
        }
        else -> TyUnknown
    }
    return ty
}
