package org.move.lang.core.types.ty

import org.move.lang.core.psi.MvTypeParametersOwner
import org.move.lang.core.psi.tyInfers
import org.move.lang.core.types.infer.*

enum class Ability {
    DROP, COPY, STORE, KEY;

    fun label(): String = this.name.lowercase()

    fun requires(): Ability {
        return when (this) {
            DROP -> DROP
            COPY -> COPY
            KEY, STORE -> STORE
        }
    }

    override fun toString(): String {
        return super.toString().lowercase()
    }

    companion object {
        fun none(): Set<Ability> = setOf()
        fun all(): Set<Ability> = setOf(DROP, COPY, STORE, KEY)
    }
}

//val emptySubstitution: Substitution = emptyMap()

val TypeFoldable<*>.hasTyInfer get() = true
//val TypeFoldable<*>.hasTyInfer get() = visitWith { it is TyInfer }

val TypeFoldable<*>.needsSubst get(): Boolean = true
//val TypeFoldable<*>.needsSubst get(): Boolean = visitWith { it is TyTypeParameter }

abstract class Ty(val flags: TypeFlags = 0) : TypeFoldable<Ty> {

    override fun foldWith(folder: TypeFolder): Ty = folder(this)

    override fun innerFoldWith(folder: TypeFolder): Ty = this

    override fun visitWith(visitor: TypeVisitor): Boolean = visitor(this)

    override fun innerVisitWith(visitor: TypeVisitor): Boolean = false

    /**
     * Bindings between formal type parameters and actual type arguments.
     */
    open val typeParameterValues: Substitution get() = emptySubstitution

    /**
     * User visible string representation of a type
     */
    abstract override fun toString(): String

    abstract fun abilities(): Set<Ability>
}

val Ty.isTypeParam: Boolean get() = this is TyInfer || this is TyTypeParameter

fun Ty.mslTy(): Ty = if (this is TyReference && this.msl) this.innermostTy() else this

abstract class GenericTy(
    open val item: MvTypeParametersOwner,
    open val substitution: Substitution,
    flags: TypeFlags,
) : Ty(flags) {

    fun withTyInfers(): GenericTy {
        val tyInfers = this.item.tyInfers
        return this.foldTyTypeParameterWith { tyInfers[it] ?: it } as GenericTy
    }
}
