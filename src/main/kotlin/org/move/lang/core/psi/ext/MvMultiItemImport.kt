package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvMultiItemImport

val MvMultiItemImport.names get() = this.itemImportList.mapNotNull { it.identifier.text }.toSet()
