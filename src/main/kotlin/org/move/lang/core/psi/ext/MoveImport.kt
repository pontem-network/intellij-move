package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveImport
import org.move.lang.core.psi.MoveImportAlias
import org.move.lang.core.psi.MoveItemImport
import org.move.lang.core.psi.MoveModuleImport

fun MoveImport.aliases(): List<MoveImportAlias> =
    when (this) {
        is MoveItemImport -> this.importedItemList.mapNotNull { it.importAlias }
        is MoveModuleImport -> listOfNotNull(this.importAlias)
        else -> emptyList()
    }