package org.move.lang.core.psi

import org.move.lang.core.psi.ext.address

interface MvImportsOwner : MvElement {
    val useStmtList: List<MvUseStmt>
}

fun MvImportsOwner.items(): Sequence<MvElement> {
    return generateSequence(firstChild) { it.nextSibling }
        .filterIsInstance<MvElement>()
        .filter { it !is MvAttr }
}

fun MvImportsOwner.moduleImports(): List<MvModuleUseSpeck> =
    useStmtList.mapNotNull { it.moduleUseSpeck }

fun MvImportsOwner.moduleImportNames(): List<MvNamedElement> =
    listOf(
        moduleImportsNoAliases(),
        moduleImportsAliases(),
    ).flatten()

fun MvImportsOwner.moduleImportsNoAliases(): List<MvModuleUseSpeck> =
    moduleImports()
        .filter { it.useAlias == null }

fun MvImportsOwner.moduleImportsAliases(): List<MvUseAlias> =
    moduleImports().mapNotNull { it.useAlias }

fun MvImportsOwner.itemImports(): List<MvUseItem> =
    useStmtList
        .mapNotNull { it.itemUseSpeck }
        .flatMap {
            val item = it.useItem
            if (item != null) {
                listOf(item)
            } else
                it.useItemGroup?.useItemList.orEmpty()
        }

fun MvImportsOwner.itemImportNames(): List<MvNamedElement> =
    listOf(
        itemImportsNoAliases(),
        itemImportsAliases(),
    ).flatten()

fun MvImportsOwner.itemImportsNoAliases(): List<MvUseItem> =
    itemImports().filter { it.useAlias == null }

fun MvImportsOwner.itemImportsAliases(): List<MvUseAlias> = itemImports().mapNotNull { it.useAlias }

fun MvImportsOwner.selfItemImports(): List<MvUseItem> =
    itemImports()
        .filter { it.useAlias == null && it.text == "Self" }

fun MvImportsOwner.itemImportsWithoutAliases(): List<MvUseItem> =
    itemImports().filter { it.useAlias == null }


fun MvImportsOwner.shortestPathText(item: MvNamedElement): String? {
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
