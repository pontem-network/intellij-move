package org.move.lang.core.psi

import org.move.lang.core.psi.ext.definedAddressRef

interface MvUseStmtOwner : MvElement {
    val useStmts: List<MvUseStmt>

    private fun moduleImportsInner(): List<MvModuleUse> =
        useStmts.mapNotNull { it.moduleUse }

    fun moduleImports(): List<MvModuleUse> =
        moduleImportsInner()
            .filter { it.useAlias == null }

    fun selfItemImports(): List<MvItemUse> =
        itemImports()
            .filter { it.useAlias == null && it.text == "Self" }

    fun moduleImportAliases(): List<MvUseAlias> =
        moduleImportsInner().mapNotNull { it.useAlias }

    private fun itemImports(): List<MvItemUse> =
        useStmts
            .mapNotNull { it.moduleItemUse }
            .flatMap {
                val item = it.itemUse
                if (item != null) {
                    listOf(item)
                } else
                    it.multiItemUse?.itemUseList.orEmpty()
            }

    fun itemImportsWithoutAliases(): List<MvItemUse> =
        itemImports().filter { it.useAlias == null }

    fun itemImportsAliases(): List<MvUseAlias> =
        itemImports().mapNotNull { it.useAlias }
}

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
    for (moduleImport in this.moduleImports()) {
        val importedModule = moduleImport.fqModuleRef?.reference?.resolve() ?: continue
        if (importedModule == module) {
            return "$moduleName::$itemName"
        }
    }
    val addressName = module.definedAddressRef()?.text ?: return null
    return "$addressName::$moduleName::$itemName"
}
