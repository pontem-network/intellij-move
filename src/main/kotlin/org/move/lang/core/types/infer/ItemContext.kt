package org.move.lang.core.types.infer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionGuard
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*

private val guard: RecursionGuard<PsiElement> =
    RecursionManager.createGuard("org.move.lang.core.types.RecursiveStructs")

@Service(Service.Level.PROJECT)
class DefaultItemContextService(val project: Project) : UserDataHolderBase() {
    fun get(msl: Boolean): ItemContext {
        val cacheManager = CachedValuesManager.getManager(project)
        return if (msl) {
            cacheManager.getCachedValue(this) {
                val builtinModule = project.psiFactory.inlineModule("0x1", "builtins", "")
                val itemContext = ItemContext(true, builtinModule)
                CachedValueProvider.Result.create(itemContext, PsiModificationTracker.MODIFICATION_COUNT)
            }
        } else {
            cacheManager.getCachedValue(this) {
                val builtinModule = project.psiFactory.inlineModule("0x1", "builtins", "")
                val itemContext = ItemContext(false, builtinModule)
                CachedValueProvider.Result.create(itemContext, PsiModificationTracker.MODIFICATION_COUNT)
            }
        }
    }
}

fun Project.itemContext(msl: Boolean): ItemContext = service<DefaultItemContextService>().get(msl)

class ItemContext(val msl: Boolean, val owner: ItemContextOwner) {
    val tyTemplateMap = mutableMapOf<MvNameIdentifierOwner, TyTemplate>()
    val constTyMap = mutableMapOf<MvConst, Ty>()

    val typeErrors = mutableListOf<TypeError>()

    fun getConstTy(const: MvConst): Ty {
        val existing = this.constTyMap[const]
        if (existing != null) {
            return existing
        } else {
            val ty = const.typeAnnotation?.type
                ?.let { inferItemTypeTy(it, this) } ?: TyUnknown
            this.constTyMap[const] = ty
            return ty
        }
    }

    // nullability happens if struct is recursive
    fun getStructItemTy(struct: MvStruct): TyStruct? = getItemTy(struct) as? TyStruct

    fun getStructFieldItemTy(structField: MvStructField): Ty {
        val struct = structField.struct
        val fieldName = structField.name ?: return TyUnknown
        val structItemTy = getStructItemTy(struct) ?: return TyUnknown
        return structItemTy.fieldTy(fieldName)
    }

    fun getFunctionItemTy(function: MvFunctionLike): TyFunction = getItemTy(function) as TyFunction

    fun getItemTy(namedItem: MvNameIdentifierOwner): Ty {
        return when (namedItem) {
            is MvStruct -> {
                val tyTemplate = this.getTemplateTy(namedItem)
                val structTyTemplate = tyTemplate as? TyTemplate.Struct ?: return TyUnknown
                return structTyTemplate.toTy(this)
            }
            is MvFunctionLike -> {
                val templateTy = this.getTemplateTy(namedItem)
                val template = templateTy as? TyTemplate.Function ?: return TyUnknown
                return template.toTy(this)
            }
            else -> TyUnknown
        }
    }

    fun getTemplateTy(namedItem: MvNameIdentifierOwner): TyTemplate {
        val existing = this.tyTemplateMap[namedItem]
        val itemTy = if (existing == null) {
            val itemTyTemplate =
                guard.doPreventingRecursion(namedItem, false) { tyTemplate(namedItem, this) }
                    ?: return TyTemplate.Unknown
            this.tyTemplateMap[namedItem] = itemTyTemplate
            itemTyTemplate
        } else {
            existing
        }
        return itemTy
    }
}

fun getItemContext(owner: ItemContextOwner, msl: Boolean): ItemContext {
    val itemContext = ItemContext(msl, owner)
    when (owner) {
        is MvModule -> {
            val moduleItems = owner.structs() +
                    owner.allNonTestFunctions().filter { it.visibility == FunctionVisibility.PUBLIC }
            for (item in moduleItems) {
                itemContext.tyTemplateMap[item] = tyTemplate(item, itemContext)
            }

            val consts = owner.consts()
            for (const in consts) {
                itemContext.constTyMap[const] =
                    const.typeAnnotation?.type
                        ?.let { inferItemTypeTy(it, itemContext) } ?: TyUnknown
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
    ) : TyTemplate() {
        override fun toTy(itemContext: ItemContext): Ty {
            val typeVars = item.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }
            val fieldTys = this.fieldTys.mapValues { (_, ty) ->
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
    ) : TyTemplate() {
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

    object Unknown : TyTemplate() {
        override fun toTy(itemContext: ItemContext): Ty = TyUnknown
    }
}

private fun tyTemplate(item: MvNameIdentifierOwner, itemContext: ItemContext): TyTemplate {
    return when (item) {
        is MvStruct -> {
            val fieldTys = mutableMapOf<String, Ty>()
            for (field in item.fields) {
                val fieldName = field.name ?: continue
                val fieldTy = field.typeAnnotation
                    ?.type
                    ?.let { inferItemTypeTy(it, itemContext) }
                    ?: TyUnknown
                fieldTys[fieldName] = fieldTy
            }
            TyTemplate.Struct(item, fieldTys)
        }

        is MvFunctionLike -> {
            val paramTypes = mutableListOf<Ty>()
            for (param in item.parameters) {
                val paramType = param.typeAnnotation?.type
                    ?.let { inferItemTypeTy(it, itemContext) } ?: TyUnknown
                paramTypes.add(paramType)
            }
            val returnType = item.returnType?.type
            val retTy = if (returnType == null) {
                TyUnit
            } else {
                val returnTy = inferItemTypeTy(returnType, itemContext)
                returnTy
            }
            val acqTys = item.acquiresPathTypes.map {
                val acqItem =
                    it.path.reference?.resolve() as? MvNameIdentifierOwner ?: return@map TyUnknown
                when (acqItem) {
                    is MvStruct -> itemContext.getStructItemTy(acqItem) ?: TyUnknown
                    is MvTypeParameter -> TyTypeParameter(acqItem)
                    else -> TyUnknown
                }
            }
            TyTemplate.Function(item, paramTypes, retTy, acqTys)
        }
        else -> TyTemplate.Unknown
    }
}

fun MvNameIdentifierOwner.outerItemContext(msl: Boolean): ItemContext {
    val itemContext = when (this) {
        is MvConst -> {
            // TODO: add ItemContextOwner to MvScript
            this.module?.itemContext(msl)
        }
        is MvFunctionLike -> {
            // TODO: add ItemContextOwner to MvScript and to MvModuleSpec
            this.module?.itemContext(msl)
        }
        is MvStruct -> this.module.itemContext(msl)
        is MvStructField -> this.struct.module.itemContext(msl)
        else -> null
    }
    return itemContext ?: project.itemContext(msl)
}
