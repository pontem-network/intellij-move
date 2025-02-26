package org.move.lang.core.resolve

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve2.itemEntries
import org.move.lang.core.types.infer.deepFoldTyTypeParameterWith
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyReference
import org.move.lang.moveProject

fun processMethodResolveVariants(
    methodOrField: MvMethodOrField,
    receiverTy: Ty,
    msl: Boolean,
    processor: RsResolveProcessor
): Boolean {
    val moveProject = methodOrField.moveProject ?: return false
    val itemModule = receiverTy.itemModule(moveProject) ?: return false
    return processor
        .wrapWithFilter { e ->
            val function = e.element as? MvFunction ?: return@wrapWithFilter false
            val selfTy = function.selfParamTy(msl) ?: return@wrapWithFilter false
            // need to use TyVar here, loweredType() erases them
            val selfTyWithTyVars =
                selfTy.deepFoldTyTypeParameterWith { tp -> TyInfer.TyVar(tp) }
            TyReference.isCompatibleWithAutoborrow(receiverTy, selfTyWithTyVars, msl)
        }
        .processAll(itemModule.allNonTestFunctions().mapNotNull { it.asEntry() })
}

fun getImportableItemsAsEntries(module: MvModule): List<ScopeEntry> {
    return module.itemEntries + module.itemEntriesFromModuleSpecs + module.globalVariableEntries
}

val MvModule.itemEntriesFromModuleSpecs: List<ScopeEntry> get() {
    val module = this
    return buildList {
        for (moduleSpec in module.allModuleSpecs()) {
            addAll(moduleSpec.specFunctions().asEntries())
            addAll(moduleSpec.specInlineFunctionsFromModuleItemSpecs().asEntries())
            addAll(moduleSpec.schemas().asEntries())
        }
    }
}
