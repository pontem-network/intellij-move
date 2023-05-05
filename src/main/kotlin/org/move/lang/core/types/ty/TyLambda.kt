package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.types.infer.mergeFlags

// TODO: inherit from GenericTy ?
interface TyCallable {
    val paramTypes: List<Ty>
    val retType: Ty
}

data class TyLambda(
    override val paramTypes: List<Ty>,
    override val retType: Ty
) : Ty(mergeFlags(paramTypes) or retType.flags), TyCallable {

    override fun abilities(): Set<Ability> = emptySet()

    override fun toString(): String = tyToString(this)
}
