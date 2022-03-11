package org.move.lang.core.psi

import org.move.lang.core.psi.ext.definedAddressRef

interface MvImportsOwner : MvElement {
    val importStmts: List<MvImportStmt>

    private fun _moduleImports(): List<MvModuleImport> =
        importStmts.mapNotNull { it.moduleImport }

    fun moduleImports(): List<MvModuleImport> =
        _moduleImports()
            .filter { it.importAlias == null }

    fun selfItemImports(): List<MvItemImport> =
        itemImports()
            .filter { it.importAlias == null && it.text == "Self" }
//            .map { it.parent }
//            .filterIsInstance<MvModuleItemsImport>()
//            .map { it.fqModuleRef }

    fun moduleImportAliases(): List<MvImportAlias> =
        _moduleImports().mapNotNull { it.importAlias }

    private fun itemImports(): List<MvItemImport> =
        importStmts
            .mapNotNull { it.moduleItemsImport }
            .flatMap {
                val item = it.itemImport
                if (item != null) {
                    listOf(item)
                } else
                    it.multiItemImport?.itemImportList.orEmpty()
            }

    fun itemImportsWithoutAliases(): List<MvItemImport> =
        itemImports().filter { it.importAlias == null }

    fun itemImportsAliases(): List<MvImportAlias> =
        itemImports().mapNotNull { it.importAlias }
}

fun MvImportsOwner.shortestPathIdentText(item: MvNamedElement): String? {
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
