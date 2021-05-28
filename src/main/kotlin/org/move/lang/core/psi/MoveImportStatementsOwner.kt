package org.move.lang.core.psi

interface MoveImportStatementsOwner : MoveElement {
    val importStatements: List<MoveImportStatement>

    @JvmDefault
    private fun _moduleImports(): List<MoveModuleImport> =
        importStatements.mapNotNull { it.moduleImport }

    @JvmDefault
    fun moduleImports(): List<MoveModuleImport> =
        _moduleImports()
            .filter { it.importAlias == null }

//    @JvmDefault
//    fun selfItemImports(): List<MoveItemImport> =
//        itemImports()
//            .filter { it.importAlias == null && it.text == "Self" }
//            .map { it.parent }
//            .filterIsInstance<MoveModuleItemsImport>()
//            .map { it.fullyQualifiedModuleRef }

    @JvmDefault
    fun moduleImportAliases(): List<MoveImportAlias> =
        _moduleImports().mapNotNull { it.importAlias }

    @JvmDefault
    private fun itemImports(): List<MoveItemImport> =
        importStatements
            .mapNotNull { it.moduleItemsImport }
            .flatMap {
                val item = it.itemImport
                if (item != null) {
                    listOf(item)
                } else
                    it.multiItemImport?.itemImportList.orEmpty()
            }

    @JvmDefault
    fun itemImportsWithoutAliases(): List<MoveItemImport> =
        itemImports().filter { it.importAlias == null }

    @JvmDefault
    fun itemImportsAliases(): List<MoveImportAlias> =
        itemImports().mapNotNull { it.importAlias }

}
