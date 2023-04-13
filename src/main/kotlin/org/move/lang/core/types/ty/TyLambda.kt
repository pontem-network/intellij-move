package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString

data class TyLambda(
    val paramTypes: List<Ty>,
    val returnType: Ty
) : Ty() {

    override fun abilities(): Set<Ability> = emptySet()

    override fun toString(): String = tyToString(this)
}
