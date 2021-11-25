package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MoveStructSignature
import org.move.lang.core.psi.ext.abilities
import org.move.lang.core.psi.ext.ability
import org.move.lang.core.types.Ability

class TyStruct(val item: MoveStructSignature, val typeArguments: List<Ty> = emptyList()) : Ty {
    override fun abilities(): Set<Ability> {
        return this.item.abilities.mapNotNull { it.ability }.toSet()
    }

    override fun toString(): String = tyToString(this)
}
