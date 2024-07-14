package org.move.lang.core.psi.ext

import com.intellij.psi.util.CachedValuesManager.getProjectPsiDependentCache
import org.move.lang.core.psi.*

interface MvItemsOwner: MvElement {
    val useStmtList: List<MvUseStmt> get() = emptyList()
}

fun MvItemsOwner.items(): Sequence<MvElement> {
    return generateSequence(firstChild) { it.nextSibling }
        .filterIsInstance<MvElement>()
        .filter { it !is MvAttr }
}

val MvItemsOwner.visibleItems: Sequence<MvItemElement>
    get() {
        return this.items()
            .filterIsInstance<MvItemElement>()
            .filterNot { (it as? MvFunction)?.hasTestAttr ?: false }
    }

fun MvItemsOwner.moduleUseItems(): List<MvNamedElement> =
    listOf(
        moduleUseSpecksNoAliases(),
        moduleUseSpecksAliases(),
        selfModuleUseItemNoAliases(),
        selfModuleUseItemAliases(),
    ).flatten()

fun MvItemsOwner.moduleUseSpecksNoAliases(): List<MvModuleUseSpeck> =
    moduleUseSpecks()
        .filter { it.useAlias == null }

fun MvItemsOwner.moduleUseSpecksAliases(): List<MvUseAlias> =
    moduleUseSpecks().mapNotNull { it.useAlias }


private fun MvItemsOwner.moduleUseSpecks(): List<MvModuleUseSpeck> {
    return getProjectPsiDependentCache(this) {
        useStmtList.mapNotNull { it.moduleUseSpeck }
    }
}

fun MvItemsOwner.psiUseItems(): List<MvUseItem> {
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

fun MvItemsOwner.allUseItems(): List<MvNamedElement> =
    listOf(
        useItemsNoAliases(),
        useItemsAliases(),
    ).flatten()

fun MvItemsOwner.useItemsNoAliases(): List<MvUseItem> =
    psiUseItems()
        .filter { !it.isSelf }
        .filter { it.useAlias == null }

fun MvItemsOwner.useItemsAliases(): List<MvUseAlias> =
    psiUseItems()
        .filter { !it.isSelf }
        .mapNotNull { it.useAlias }

fun MvItemsOwner.selfModuleUseItemNoAliases(): List<MvUseItem> =
    psiUseItems()
        .filter { it.isSelf && it.useAlias == null }

fun MvItemsOwner.selfModuleUseItemAliases(): List<MvUseAlias> =
    psiUseItems()
        .filter { it.isSelf }
        .mapNotNull { it.useAlias }

fun MvItemsOwner.shortestPathText(item: MvNamedElement): String? {
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
