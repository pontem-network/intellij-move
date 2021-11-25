package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.psi.ext.abilities
import org.move.lang.core.psi.ext.ability
import org.move.lang.core.types.Ability

class TyTypeParameter(val parameter: MoveTypeParameter): Ty {

    val name: String? get() = parameter.name

    override fun abilities(): Set<Ability> {
        return parameter.abilities.mapNotNull { it.ability }.toSet()
    }

    override fun toString(): String = tyToString(this)
}
