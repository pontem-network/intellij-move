package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.types.infer.DAGNode
import org.move.lang.core.types.infer.DAGNodeOrValue
import org.move.lang.core.types.infer.DAGValue

sealed class TyInfer : Ty {
    class TyVar(
        val origin: TyTypeParameter? = null,
        override var next: DAGNodeOrValue = DAGValue(null)
    ) : TyInfer(), DAGNode {
        override fun abilities(): Set<Ability> = origin?.abilities() ?: Ability.none()
    }

    class IntVar(
        override var next: DAGNodeOrValue = DAGValue(null)
    ) : TyInfer(), DAGNode {
        override fun abilities(): Set<Ability> = Ability.all()
    }

    override fun toString(): String = tyToString(this)
}
