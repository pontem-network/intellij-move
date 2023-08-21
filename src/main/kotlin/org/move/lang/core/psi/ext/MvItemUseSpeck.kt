package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvItemUseSpeck

fun MvItemUseSpeck.names(): List<String> =
    this.useItemGroup?.names ?: listOfNotNull(this.useItem?.name)
