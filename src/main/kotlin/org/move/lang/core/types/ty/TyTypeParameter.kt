package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.ext.abilityBounds
import org.move.lang.core.psi.ext.ability


data class TyTypeParameter(val origin: MvTypeParameter) : Ty() {

    val name: String? get() = origin.name

    override fun abilities(): Set<Ability> {
        return origin.abilityBounds.mapNotNull { it.ability }.toSet()
    }

    override fun toString(): String = tyToString(this)
}
