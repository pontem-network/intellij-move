package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.types.infer.HAS_TY_INFER_MASK

sealed interface VarOrValue
data class VarValue(val ty: Ty?): VarOrValue

sealed class TyInfer: Ty(HAS_TY_INFER_MASK), VarOrValue {

    abstract var parent: VarOrValue

    // Note these classes must NOT be `data` classes and must provide equality by identity
    class TyVar(
        val origin: TyTypeParameter? = null,
        override var parent: VarOrValue = VarValue(null),
    ): TyInfer() {

        override fun abilities(): Set<Ability> = origin?.abilities() ?: Ability.none()
    }

    class IntVar(
        override var parent: VarOrValue = VarValue(null)
    ): TyInfer() {

        override fun abilities(): Set<Ability> = Ability.all()
    }

    override fun toString(): String = tyToString(this)
}
