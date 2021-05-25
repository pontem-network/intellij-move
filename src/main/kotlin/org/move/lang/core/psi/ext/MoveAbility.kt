package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveAbility
import org.move.lang.core.types.Ability

val MoveAbility.ability: Ability?
    get() =
        when (this.text) {
            "copy" -> Ability.COPY
            "store" -> Ability.STORE
            "key" -> Ability.KEY
            "drop" -> Ability.DROP
            else -> null
        }
