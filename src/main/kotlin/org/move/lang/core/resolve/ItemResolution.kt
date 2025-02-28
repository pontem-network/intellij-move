package org.move.lang.core.resolve

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.resolve2.itemEntries
import org.move.lang.core.types.infer.deepFoldTyTypeParameterWith
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyReference
import org.move.lang.moveProject

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
            val selfParameter = (functionEntry.element as MvFunction).selfParam ?: continue
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
