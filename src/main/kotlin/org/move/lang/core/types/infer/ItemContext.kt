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
    val tyTemplateMap = mutableMapOf<MvNameIdentifierOwner, TyTemplate>()
    val typeTyMap = mutableMapOf<MvType, Ty>()

    val typeErrors = mutableListOf<TypeError>()

    fun getTypeTy(type: MvType): Ty {
        return inferItemTypeTy(type, this)
    }

    fun getBuiltinTypeTy(pathType: MvPathType): Ty {
        val existing = this.typeTyMap[pathType]
        if (existing != null) {
            return existing
        } else {
            val ty = inferItemBuiltinTypeTy(pathType, this)
            this.typeTyMap[pathType] = ty
            return ty
        }
    }

    fun getItemTy(namedItem: MvNameIdentifierOwner): Ty {
        return when (namedItem) {
            is MvStruct -> {
                val existing = this.tyTemplateMap[namedItem]
                val tyTemplate = if (existing == null) {
                    val itemTyTemplate = tyTemplate(namedItem, this)
                    this.tyTemplateMap[namedItem] = itemTyTemplate
                    itemTyTemplate
                } else {
                    existing
                }
                val structTyTemplate = tyTemplate as? TyTemplate.Struct ?: return TyUnknown
                return structTyTemplate.toTy(this)
            }
            is MvFunctionLike -> {
                val existing = this.tyTemplateMap[namedItem]
                val itemTy = if (existing == null) {
                    val itemTyTemplate = tyTemplate(namedItem, this)
                    this.tyTemplateMap[namedItem] = itemTyTemplate
                    itemTyTemplate
                } else {
                    existing
                }
                val template = itemTy as? TyTemplate.Function ?: return TyUnknown
                return template.toTy(this)
            }
            else -> TyUnknown
        }
    }
}

private fun getItemContext(owner: ItemContextOwner, msl: Boolean): ItemContext {
    val itemContext = ItemContext(msl)
    when (owner) {
        is MvModule -> {
            val moduleItems = owner.structs().asSequence() +
                    owner.allNonTestFunctions().asSequence() +
                    owner.specFunctions().asSequence()
            for (item in moduleItems) {
                itemContext.tyTemplateMap[item] = tyTemplate(item, itemContext)
            }
        }
    }
    return itemContext
}

private fun findTypeVarForParam(typeVars: List<TyInfer.TyVar>, param: MvTypeParameter): Ty {
    val typeVar = typeVars.find { it.origin?.origin == param }
    if (typeVar == null) {
        val owner = param.parent.parent as? MvStruct
        error("no typeVar for parameter \"${param.text}\" in $typeVars (of item ${owner?.fqName})")
    }
    return typeVar
}

sealed class TyTemplate {
    abstract fun toTy(itemContext: ItemContext): Ty

    data class Struct(
        val item: MvStruct,
        val fieldTys: Map<String, Ty>,
    ): TyTemplate() {
        override fun toTy(itemContext: ItemContext): Ty {
            val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }
            val fieldTys = this.fieldTys.mapValues {(_, ty) ->
                ty.foldTyTypeParameterWith { findTypeVarForParam(typeVars, it.origin) }
            }
            val typeArgs = typeVars.toList()
            return TyStruct(item, typeVars, fieldTys, typeArgs)
        }
    }

    data class Function(
        val item: MvFunctionLike,
        val paramTys: List<Ty>,
        val returnTy: Ty,
        val acqTys: List<Ty>,
    ): TyTemplate() {
        override fun toTy(itemContext: ItemContext): Ty {
            val typeVars = this.item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }
            val paramTys = this.paramTys.map {
                it.foldTyTypeParameterWith { tp -> findTypeVarForParam(typeVars, tp.origin) }
            }
            val returnTy = this.returnTy.foldTyTypeParameterWith { findTypeVarForParam(typeVars, it.origin) }
            val acqTys = this.acqTys.map {
                it.foldTyTypeParameterWith { tp -> findTypeVarForParam(typeVars, tp.origin) }
            }
            val typeArgs = typeVars.toList()
            return TyFunction(item, typeVars, paramTys, returnTy, acqTys, typeArgs)
        }
    }

    object Unknown: TyTemplate() {
        override fun toTy(itemContext: ItemContext): Ty = TyUnknown
    }
}

private fun tyTemplate(item: MvNameIdentifierOwner, itemContext: ItemContext): TyTemplate {
    return when (item) {
        is MvStruct -> {
            val fieldTys = mutableMapOf<String, Ty>()
            for (field in item.fields) {
                val fieldName = field.name ?: return TyTemplate.Unknown
                val fieldTy = field.typeAnnotation
                    ?.type
                    ?.let { itemContext.getTypeTy(it) }
//                    ?.let { inferItemTypeTy(it, itemContext) }
                    ?: TyUnknown
                fieldTys[fieldName] = fieldTy
            }

            TyTemplate.Struct(item, fieldTys)
        }

        is MvFunctionLike -> {
            val paramTypes = mutableListOf<Ty>()
            for (param in item.parameters) {
                val paramType = param.typeAnnotation?.type
                    ?.let { itemContext.getTypeTy(it) } ?: TyUnknown
//                    ?.let { inferItemTypeTy(it, itemContext) } ?: TyUnknown
                paramTypes.add(paramType)
            }
            val returnMvType = item.returnType?.type
            val retTy = if (returnMvType == null) {
                TyUnit
            } else {
                val returnTy = itemContext.getTypeTy(returnMvType)
//                val returnTy = inferItemTypeTy(returnMvType, itemContext)
                returnTy
            }
            val acqTys = item.acquiresPathTypes.map {
                val acqItem =
                    it.path.reference?.resolve() as? MvNameIdentifierOwner ?: return@map TyUnknown
                when (acqItem) {
                    is MvStruct -> itemContext.getItemTy(acqItem)
                    is MvTypeParameter -> TyTypeParameter(acqItem)
                    else -> TyUnknown
                }
            }
            TyTemplate.Function(item, paramTypes, retTy, acqTys)
        }

//        is MvTypeParameter -> TyTypeParameter(item)
        else -> TyTemplate.Unknown
    }
}

private fun inferItemTypeTy(
    moveType: MvType,
    itemContext: ItemContext,
): Ty {
//    val existing = itemContext.typeTyMap[moveType]
//    if (existing != null) {
//        return existing
//    }
    val ty = when (moveType) {
        is MvPathType -> run {
            val namedItem =
                moveType.path.reference?.resolve() ?: return@run itemContext.getBuiltinTypeTy(moveType)
//                moveType.path.reference?.resolve() ?: return@run inferItemBuiltinTypeTy(moveType, itemContext)
            when (namedItem) {
                is MvTypeParameter -> TyTypeParameter(namedItem)
                is MvStruct -> {
                    // check that it's not a recursive type
                    val parentStruct = moveType.parentOfType<MvStruct>()
                    if (parentStruct != null && namedItem == parentStruct) {
                        itemContext.typeErrors.add(TypeError.CircularType(moveType, parentStruct))
                        return TyUnknown
                    }

                    val typeArgs = moveType.path.typeArguments.map { itemContext.getTypeTy(it.type) }
//                        .map { inferItemTypeTy(it.type, itemContext) }

                    val rawStructTy = itemContext.getItemTy(namedItem) as? TyStruct ?: return TyUnknown

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
            val innerTy = itemContext.getTypeTy(innerTypeRef)
//            val innerTy = inferItemTypeTy(innerTypeRef, itemContext)
            TyReference(innerTy, mutabilities, itemContext.msl)
        }
        is MvTupleType -> {
            val innerTypes = moveType.typeList.map { itemContext.getTypeTy(it) }
//            val innerTypes = moveType.typeList.map { inferItemTypeTy(it, itemContext) }
            TyTuple(innerTypes)
        }
        is MvUnitType -> TyUnit
        else -> TyUnknown
    }
//    itemContext.typeTyMap[moveType] = ty
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
                ?.let { itemContext.getTypeTy(it) } ?: TyUnknown
//                ?.let { inferItemTypeTy(it, itemContext) } ?: TyUnknown
            return TyVector(itemTy)
        }
        else -> TyUnknown
    }

//    itemContext.typeTyMap[pathType] = ty
    return ty
}
