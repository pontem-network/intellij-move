package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MoveTypeParameter

class TyTypeParameter(val parameter: MoveTypeParameter): Ty {

    val name: String? get() = parameter.name

    override fun toString(): String = tyToString(this)
}
