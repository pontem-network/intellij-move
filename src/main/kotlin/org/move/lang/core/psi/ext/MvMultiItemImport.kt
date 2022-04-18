package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvMultiItemUse

val MvMultiItemUse.names get() = this.itemUseList.mapNotNull { it.identifier.text }.toSet()
