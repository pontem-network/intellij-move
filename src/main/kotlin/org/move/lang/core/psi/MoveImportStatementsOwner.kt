package org.move.lang.core.psi

interface MoveImportStatementsOwner : MoveElement {
    val importStatements: List<MoveImportStatement>

    @JvmDefault
    private fun moduleImports(): List<MoveModuleImport> =
        importStatements.mapNotNull { it.moduleImport }

    @JvmDefault
    fun moduleImportsWithoutAliases(): List<MoveModuleImport> =
        moduleImports().filter { it.importAlias == null }

    @JvmDefault
    fun moduleImportAliases(): List<MoveImportAlias> =
        moduleImports().mapNotNull { it.importAlias }

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