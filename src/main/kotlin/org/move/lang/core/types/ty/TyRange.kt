package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString

data class TyRange(val item: Ty): Ty() {
    override fun abilities(): Set<Ability> = Ability.all()
    override fun toString(): String = tyToString(this)
}
