package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvItemUseSpeck
import org.move.lang.core.psi.MvUseItemGroup

fun MvItemUseSpeck.names(): List<String> =
    this.useItemGroup?.names ?: listOfNotNull(this.useItem?.name)

val MvUseItemGroup.names get() = this.useItemList.mapNotNull { it.identifier.text }
