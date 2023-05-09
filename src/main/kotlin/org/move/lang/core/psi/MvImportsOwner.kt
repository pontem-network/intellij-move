package org.move.lang.core.psi

import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.lang.core.psi.ext.addressRef
import org.move.lang.core.psi.ext.isSelf

interface MvImportsOwner : MvElement {
    val useStmtList: List<MvUseStmt>
}

fun MvImportsOwner.items(): Sequence<MvElement> {
    return generateSequence(firstChild) { it.nextSibling }
        .filterIsInstance<MvElement>()
        .filter { it !is MvAttr }
}

fun MvImportsOwner.moduleUseSpecks(): List<MvModuleUseSpeck> {
    return getProjectPsiDependentCache(this) {
        useStmtList.mapNotNull { it.moduleUseSpeck }
    }
}

fun MvImportsOwner.allModuleUseSpecks(): List<MvNamedElement> =
    listOf(
        moduleUseSpecksNoAliases(),
        moduleUseSpecksAliases(),
    ).flatten()

fun MvImportsOwner.moduleUseSpecksNoAliases(): List<MvModuleUseSpeck> =
    moduleUseSpecks()
        .filter { it.useAlias == null }

fun MvImportsOwner.moduleUseSpecksAliases(): List<MvUseAlias> =
    moduleUseSpecks().mapNotNull { it.useAlias }

fun MvImportsOwner.psiUseItems(): List<MvUseItem> {
    return getProjectPsiDependentCache(this) { importsOwner ->
        importsOwner
            .useStmtList
            .mapNotNull { it.itemUseSpeck }
            .flatMap {
                val item = it.useItem
                if (item != null) {
                    listOf(item)
                } else
                    it.useItemGroup?.useItemList.orEmpty()
            }

    }
}

fun MvImportsOwner.allUseItems(): List<MvNamedElement> =
    listOf(
        useItemsNoAliases(),
        useItemsAliases(),
    ).flatten()

fun MvImportsOwner.useItemsNoAliases(): List<MvUseItem> =
    psiUseItems()
        .filter { !it.isSelf }
        .filter { it.useAlias == null }

fun MvImportsOwner.useItemsAliases(): List<MvUseAlias> =
    psiUseItems()
        .filter { !it.isSelf }
        .mapNotNull { it.useAlias }

fun MvImportsOwner.selfModuleUseItemNoAliases(): List<MvUseItem> =
    psiUseItems()
        .filter { it.isSelf && it.useAlias == null }

fun MvImportsOwner.selfModuleUseItemAliases(): List<MvUseAlias> =
    psiUseItems()
        .filter { it.isSelf }
        .mapNotNull { it.useAlias }

fun MvImportsOwner.shortestPathText(item: MvNamedElement): String? {
    val itemName = item.name ?: return null
    // local
    if (this == item.containingImportsOwner) return itemName

    for (useItem in this.useItemsNoAliases()) {
        val importedItem = useItem.reference.resolve() ?: continue
        if (importedItem == item) {
            return itemName
        }
    }
    val module = item.containingModule ?: return null
    val moduleName = module.name ?: return null
    for (moduleImport in this.moduleUseSpecksNoAliases()) {
        val importedModule = moduleImport.fqModuleRef?.reference?.resolve() ?: continue
        if (importedModule == module) {
            return "$moduleName::$itemName"
        }
    }
    val addressName = module.addressRef()?.text ?: return null
    return "$addressName::$moduleName::$itemName"
}
