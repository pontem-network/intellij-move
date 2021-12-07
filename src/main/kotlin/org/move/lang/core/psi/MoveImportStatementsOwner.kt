package org.move.lang.core.psi

interface MvImportStatementsOwner : MvElement {
    val importStatements: List<MvImportStatement>

    @JvmDefault
    private fun _moduleImports(): List<MvModuleImport> =
        importStatements.mapNotNull { it.moduleImport }

    @JvmDefault
    fun moduleImports(): List<MvModuleImport> =
        _moduleImports()
            .filter { it.importAlias == null }

    @JvmDefault
    fun selfItemImports(): List<MvItemImport> =
        itemImports()
            .filter { it.importAlias == null && it.text == "Self" }
//            .map { it.parent }
//            .filterIsInstance<MvModuleItemsImport>()
//            .map { it.fqModuleRef }

    @JvmDefault
    fun moduleImportAliases(): List<MvImportAlias> =
        _moduleImports().mapNotNull { it.importAlias }

    @JvmDefault
    private fun itemImports(): List<MvItemImport> =
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
    fun itemImportsWithoutAliases(): List<MvItemImport> =
        itemImports().filter { it.importAlias == null }

    @JvmDefault
    fun itemImportsAliases(): List<MvImportAlias> =
        itemImports().mapNotNull { it.importAlias }

}
