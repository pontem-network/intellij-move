package org.move.lang.core.resolve

import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.*
import org.move.lang.core.psi.selfParam
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.lang.core.resolve.scopeEntry.asEntries
import org.move.lang.core.resolve.scopeEntry.itemEntries
import org.move.lang.core.types.infer.deepFoldTyTypeParameterWith
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyReference
import org.move.lang.moveProject
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
import org.move.utils.psiCacheResult
import kotlin.collections.plus

fun getMethodResolveVariants(
    methodOrField: MvMethodOrField,
    receiverTy: Ty,
    msl: Boolean
): List<ScopeEntry> {
    return buildList {
        val moveProject = methodOrField.moveProject ?: return@buildList
        val itemModule = receiverTy.itemModule(moveProject) ?: return@buildList

        val functionEntries = itemModule.allNonTestFunctions().asEntries()
        for (functionEntry in functionEntries) {
            val f = functionEntry.element() as? MvFunction ?: continue
            val selfParameter = f.selfParam ?: continue
            val selfParameterTy = selfParameter.type?.loweredType(msl) ?: continue
            // need to use TyVar here, loweredType() erases them
            val selfTyWithTyVars =
                selfParameterTy.deepFoldTyTypeParameterWith { tp -> TyInfer.TyVar(tp) }
            if (TyReference.isCompatibleWithAutoborrow(receiverTy, selfTyWithTyVars, msl)) {
                add(functionEntry)
            }
        }
    }
}

class ImportableItemsAsEntries(override val owner: MvModule): PsiCachedValueProvider<List<ScopeEntry>> {
    override fun compute(): CachedValueProvider.Result<List<ScopeEntry>> {
        val entries = owner.itemEntries + owner.globalVariableEntries + owner.itemEntriesFromModuleSpecs
        return owner.psiCacheResult(entries)
    }
}

val MvModule.importableItemEntries: List<ScopeEntry> get() {
    return ImportableItemsAsEntries(this).getResults()
}

//fun getImportableItemsAsEntries(module: MvModule): List<ScopeEntry> {
//    return ImportableItemsAsEntries(module).getResults()
////    return module.itemEntries + module.globalVariableEntries + module.itemEntriesFromModuleSpecs
//}

val MvModule.itemEntriesFromModuleSpecs: List<ScopeEntry>
    get() {
        val module = this
        return buildList {
            val specs = module.getModuleSpecsFromIndex()
            for (spec in specs) {
                addAll(spec.schemas().asEntries())
                addAll(spec.specFunctions().asEntries())
                spec.moduleItemSpecs().forEach {
                    addAll(it.globalVariableEntries)
                    addAll(it.specInlineFunctions().asEntries())
                }
            }
        }
    }
