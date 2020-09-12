package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveImport
import org.move.lang.core.psi.MoveImportAlias
import org.move.lang.core.psi.MoveItemImport
import org.move.lang.core.psi.MoveModuleImport

fun MoveImport.aliases(): List<MoveImportAlias> =
    when (this) {
        is MoveItemImport -> {
            val multipleAliases =
                this.multiImportedItem?.importedItemList.orEmpty().mapNotNull { it.importAlias }
            val singleAlias = listOfNotNull(this.importedItem?.importAlias)
            listOf(
                singleAlias,
                multipleAliases).flatten()
        }
        is MoveModuleImport -> listOfNotNull(this.importAlias)
        else -> emptyList()
    }