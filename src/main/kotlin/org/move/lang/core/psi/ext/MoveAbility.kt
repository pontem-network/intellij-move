package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAbility
import org.move.lang.core.types.ty.Ability

val MvAbility.ability: Ability?
    get() =
        when (this.text) {
            "copy" -> Ability.COPY
            "store" -> Ability.STORE
            "key" -> Ability.KEY
            "drop" -> Ability.DROP
            else -> null
        }
