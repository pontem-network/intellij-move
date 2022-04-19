package org.move.lang.core.psi

import org.move.lang.core.psi.ext.definedAddressRef

interface MvUseStmtOwner : MvElement {
    val useStmtList: List<MvUseStmt>
}

fun MvUseStmtOwner.moduleImports(): List<MvModuleUse> =
    useStmtList.mapNotNull { it.moduleUse }

fun MvUseStmtOwner.moduleImportNames(): List<MvNamedElement> =
    listOf(
        moduleImportsNoAliases(),
        moduleImportsAliases(),
    ).flatten()

fun MvUseStmtOwner.moduleImportsNoAliases(): List<MvModuleUse> =
    moduleImports()
        .filter { it.useAlias == null }

fun MvUseStmtOwner.moduleImportsAliases(): List<MvUseAlias> =
    moduleImports().mapNotNull { it.useAlias }

fun MvUseStmtOwner.itemImports(): List<MvItemUse> =
    useStmtList
        .mapNotNull { it.moduleItemUse }
        .flatMap {
            val item = it.itemUse
            if (item != null) {
                listOf(item)
            } else
                it.multiItemUse?.itemUseList.orEmpty()
        }

fun MvUseStmtOwner.itemImportNames(): List<MvNamedElement> =
    listOf(
        itemImportsNoAliases(),
        itemImportsAliases(),
    ).flatten()

fun MvUseStmtOwner.itemImportsNoAliases(): List<MvItemUse> =
    itemImports().filter { it.useAlias == null }

fun MvUseStmtOwner.itemImportsAliases(): List<MvUseAlias>
    = itemImports().mapNotNull { it.useAlias }

fun MvUseStmtOwner.selfItemImports(): List<MvItemUse> =
    itemImports()
        .filter { it.useAlias == null && it.text == "Self" }

fun MvUseStmtOwner.itemImportsWithoutAliases(): List<MvItemUse> =
    itemImports().filter { it.useAlias == null }


fun MvUseStmtOwner.shortestPathIdentText(item: MvNamedElement): String? {
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
    val addressName = module.definedAddressRef()?.text ?: return null
    return "$addressName::$moduleName::$itemName"
}
