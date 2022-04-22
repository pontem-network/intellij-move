package org.move.lang.core.psi

import org.move.lang.core.psi.ext.address

interface MvItemsOwner : MvElement {
    val useStmtList: List<MvUseStmt>
}

fun MvItemsOwner.items(): Sequence<MvElement> {
    return generateSequence(firstChild) { it.nextSibling }
        .filterIsInstance<MvElement>()
        .filter { it !is MvAttr }
}

fun MvItemsOwner.moduleImports(): List<MvModuleUseSpeck> =
    useStmtList.mapNotNull { it.moduleUseSpeck }

fun MvItemsOwner.moduleImportNames(): List<MvNamedElement> =
    listOf(
        moduleImportsNoAliases(),
        moduleImportsAliases(),
    ).flatten()

fun MvItemsOwner.moduleImportsNoAliases(): List<MvModuleUseSpeck> =
    moduleImports()
        .filter { it.useAlias == null }

fun MvItemsOwner.moduleImportsAliases(): List<MvUseAlias> =
    moduleImports().mapNotNull { it.useAlias }

fun MvItemsOwner.itemImports(): List<MvUseItem> =
    useStmtList
        .mapNotNull { it.itemUseSpeck }
        .flatMap {
            val item = it.useItem
            if (item != null) {
                listOf(item)
            } else
                it.useItemGroup?.useItemList.orEmpty()
        }

fun MvItemsOwner.itemImportNames(): List<MvNamedElement> =
    listOf(
        itemImportsNoAliases(),
        itemImportsAliases(),
    ).flatten()

fun MvItemsOwner.itemImportsNoAliases(): List<MvUseItem> =
    itemImports().filter { it.useAlias == null }

fun MvItemsOwner.itemImportsAliases(): List<MvUseAlias> = itemImports().mapNotNull { it.useAlias }

fun MvItemsOwner.selfItemImports(): List<MvUseItem> =
    itemImports()
        .filter { it.useAlias == null && it.text == "Self" }

fun MvItemsOwner.itemImportsWithoutAliases(): List<MvUseItem> =
    itemImports().filter { it.useAlias == null }


fun MvItemsOwner.shortestPathIdentText(item: MvNamedElement): String? {
    val itemName = item.name ?: return null
    // local
    if (this == item.containingImportsOwner) return itemName

    for (itemImport in this.itemImportsWithoutAliases()) {
        val importedItem = itemImport.reference.resolve() ?: continue
        if (importedItem == item) {
            return itemName
        }
    }
    val module = item.containingModule ?: return null
    val moduleName = module.name ?: return null
    for (moduleImport in this.moduleImportsNoAliases()) {
        val importedModule = moduleImport.fqModuleRef?.reference?.resolve() ?: continue
        if (importedModule == module) {
            return "$moduleName::$itemName"
        }
    }
    val addressName = module.address()?.text ?: return null
    return "$addressName::$moduleName::$itemName"
}
