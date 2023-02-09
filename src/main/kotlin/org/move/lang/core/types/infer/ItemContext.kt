package org.move.lang.core.types.infer

import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_INTEGER_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*

interface ItemContextOwner : MvElement

val MvElement.itemContextOwner: ItemContextOwner?
    get() {
        return PsiTreeUtil.findFirstParent(this) { it is ItemContextOwner }
                as? ItemContextOwner
    }

fun ItemContextOwner.itemContext(msl: Boolean): ItemContext {
    return if (msl) {
        CachedValuesManager.getProjectPsiDependentCache(this) {
            getItemContext(it, true)
        }
    } else {
        CachedValuesManager.getProjectPsiDependentCache(this) {
            getItemContext(it, false)
        }
    }
}

class ItemContext(val msl: Boolean) {
    val rawItemTyMap = mutableMapOf<MvNameIdentifierOwner, Ty>()
    val typeTyMap = mutableMapOf<MvType, Ty>()

    val typeErrors = mutableListOf<TypeError>()

//    fun getTypeTy(type: MvType): Ty {
//        return inferItemTypeTy(type, this)
//    }

    fun getRawItemTy(namedItem: MvNameIdentifierOwner): Ty {
//        val existing = this.rawItemTyMap[namedItem]
        val existing = this.rawItemTyMap[namedItem]
        if (existing != null) {
            when (existing) {
                is TyStruct -> {
                    val typeVars = existing.item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }
                    return TyStruct(
                        existing.item,
                        typeVars,
                        fieldTys = existing.fieldTys.mapValues { (_, v) ->
                            v.foldTyTypeParameterWith {
                                findTypeVarForParam(typeVars, it.origin)
                            }
                        },
                        typeArgs = existing.item.typeParameters.map { findTypeVarForParam(typeVars, it) }
                    )
//                    return existing
//                        .foldTyInferWith { if (it is TyInfer.TyVar) TyInfer.TyVar(it.origin) else it }
//                        .foldTyTypeParameterWith { findTypeVarForParam(typeVars, it.origin) }
                }
//                is TyFunction -> {
////                    val typeVars = existing.item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }
////                    val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }
//
//                    return existing
//                        .foldTyInferWith { if (it is TyInfer.TyVar) TyInfer.TyVar(it.origin) else it }
////                        .foldTyTypeParameterWith { findTypeVarForParam(typeVars, it.origin) }
//                }
//                else -> existing
            }
        }
        val ty = rawItemTy(namedItem, this)
        this.rawItemTyMap[namedItem] = ty
        return ty
    }
}

private fun getItemContext(owner: ItemContextOwner, msl: Boolean): ItemContext {
    val itemContext = ItemContext(msl)
//    when (owner) {
//        is MvModule -> {
//            val moduleItems = owner.structs().asSequence() +
//                    owner.allNonTestFunctions().asSequence() +
//                    owner.specFunctions().asSequence()
//            for (item in moduleItems) {
//                itemContext.rawItemTyMap[item] = rawItemTy(item, itemContext)
//            }
//        }
//    }
    return itemContext
}

private fun findTypeVarForParam(typeVars: List<TyInfer.TyVar>, param: MvTypeParameter): Ty {
    val typeVar = typeVars.find { it.origin?.origin == param }
    if (typeVar == null) {
        error("No typeVar for parameter \"${param.text}\" in $typeVars")
    }
    return typeVar
}

private fun rawItemTy(item: MvNameIdentifierOwner, itemContext: ItemContext): Ty {
    return when (item) {
        is MvStruct -> {
            val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

            val fieldTys = mutableMapOf<String, Ty>()
            for (field in item.fields) {
                val fieldName = field.name ?: return TyUnknown
                val fieldTy = item
                    .fieldsMap[fieldName]
//                    ?.declarationTypeTy(inferenceCtx)
                    ?.typeAnnotation
                    ?.type
                    ?.let { inferItemTypeTy(it, itemContext) }
                    ?.foldTyTypeParameterWith { findTypeVarForParam(typeVars, it.origin) }
                    ?: TyUnknown
                fieldTys[fieldName] = fieldTy
            }

            val typeArgs = item.typeParameters.map { findTypeVarForParam(typeVars, it) }
            TyStruct(item, typeVars, fieldTys, typeArgs)
        }

        is MvFunctionLike -> {
            val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

            val paramTypes = mutableListOf<Ty>()
            for (param in item.parameters) {
                val paramType = param.typeAnnotation?.type
                    ?.let { inferItemTypeTy(it, itemContext) }
                    ?.foldTyTypeParameterWith { findTypeVarForParam(typeVars, it.origin) } ?: TyUnknown
                paramTypes.add(paramType)
            }
            val returnMvType = item.returnType?.type
            val retTy = if (returnMvType == null) {
                TyUnit
            } else {
                val returnTy = inferItemTypeTy(returnMvType, itemContext)
                returnTy
                    .foldTyTypeParameterWith { findTypeVarForParam(typeVars, it.origin) }
            }
            val acqTys = item.acquiresPathTypes.map {
                val acqItem =
                    it.path.reference?.resolve() as? MvNameIdentifierOwner ?: return@map TyUnknown
                rawItemTy(acqItem, itemContext)
                    .foldTyTypeParameterWith { tp -> findTypeVarForParam(typeVars, tp.origin) }
            }
            val typeArgs = item.typeParameters.map { findTypeVarForParam(typeVars, it) }
            TyFunction(item, typeVars, paramTypes, retTy, acqTys, typeArgs)
        }

        is MvTypeParameter -> item.ty()
        else -> TyUnknown
    }
}

private fun inferItemTypeTy(
    moveType: MvType,
    itemContext: ItemContext,
): Ty {
    val ty = when (moveType) {
        is MvPathType -> run {
            val namedItem =
                moveType.path.reference?.resolve() ?: return@run inferItemBuiltinTypeTy(moveType, itemContext)
            when (namedItem) {
                is MvTypeParameter -> TyTypeParameter(namedItem)
                is MvStruct -> {
                    // check that it's not a recursive type
                    val parentStruct = moveType.parentOfType<MvStruct>()
                    if (parentStruct != null && namedItem == parentStruct) {
                        itemContext.typeErrors.add(TypeError.CircularType(moveType, parentStruct))
                        return TyUnknown
                    }

                    val typeArgs = moveType.path.typeArguments.map { inferItemTypeTy(it.type, itemContext) }

                    val rawStructTy = itemContext.getRawItemTy(namedItem) as? TyStruct
                        ?: return TyUnknown

                    val ctx = InferenceContext(itemContext.msl)
                    if (rawStructTy.typeVars.isNotEmpty()) {
                        for ((tyVar, tyArg) in rawStructTy.typeVars.zip(typeArgs)) {
                            ctx.addConstraint(tyVar, tyArg)
                        }
                        ctx.processConstraints()
                    }
                    val structTy = ctx.resolveTy(rawStructTy)
                    structTy
                }
                else -> TyUnknown
            }
        }
        is MvRefType -> run {
            val mutabilities = RefPermissions.valueOf(moveType.mutable)
            val innerTypeRef = moveType.type
                ?: return@run TyReference(TyUnknown, mutabilities, itemContext.msl)
            val innerTy = inferItemTypeTy(innerTypeRef, itemContext)
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

private fun inferItemBuiltinTypeTy(pathType: MvPathType, itemContext: ItemContext): Ty {
//    val existingTy = itemContext.typeTyMap[pathType]
//    if (existingTy != null) {
//        return existingTy
//    }

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

//    itemContext.typeTyMap[pathType] = ty
    return ty
}
