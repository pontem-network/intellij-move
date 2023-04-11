package org.move.lang.core.types.infer

import com.intellij.psi.util.parentOfType
import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_INTEGER_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.mutable
import org.move.lang.core.psi.ext.paramTypes
import org.move.lang.core.psi.ext.returnType
import org.move.lang.core.psi.ext.typeArguments
import org.move.lang.core.types.ty.*

fun ItemContext.rawType(moveType: MvType): Ty = inferItemTypeTy(moveType, this)

fun inferItemTypeTy(moveType: MvType, itemContext: ItemContext): Ty {
    return when (moveType) {
        is MvPathType -> {
            val namedItem = moveType.path.reference?.resolveWithAliases()
                    ?: return inferItemBuiltinTypeTy(moveType, itemContext)
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

                    // TODO: use substitutions with cache somehow here
                    val ctx = InferenceContext(itemContext.msl, itemContext)
                    if (rawStructTy.typeVars.isNotEmpty()) {
                        val typeArgs = moveType.path.typeArguments.map { inferItemTypeTy(it.type, itemContext) }
                        for ((tyVar, tyArg) in rawStructTy.typeVars.zip(typeArgs)) {
                            ctx.registerEquateObligation(tyVar, tyArg)
                        }
                        ctx.processConstraints()
                    }
                    val structTy = ctx.resolveTy(rawStructTy)
//                    val structTy = ctx.resolveTy(rawStructTy)
                    structTy
                }
                else -> TyUnknown
            }
        }
        is MvRefType -> {
            val mutabilities = RefPermissions.valueOf(moveType.mutable)
            val refInnerType = moveType.type
                ?: return TyReference(TyUnknown, mutabilities, itemContext.msl)
            val innerTy = inferItemTypeTy(refInnerType, itemContext)
            TyReference(innerTy, mutabilities, itemContext.msl)
        }
        is MvTupleType -> {
            val innerTypes = moveType.typeList.map { inferItemTypeTy(it, itemContext) }
            TyTuple(innerTypes)
        }
        is MvUnitType -> TyUnit
        is MvParensType -> inferItemTypeTy(moveType.type, itemContext)
        is MvLambdaType -> {
            val paramTys = moveType.paramTypes.map { inferItemTypeTy(it, itemContext) }
            val returnType = moveType.returnType
            val retTy = if (returnType == null) {
                TyUnit
            } else {
                inferItemTypeTy(returnType, itemContext)
            }
            TyLambda(paramTys, retTy)
        }
        else -> TyUnknown
    }
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
