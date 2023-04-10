package org.move.lang.core.types.ty

import org.move.lang.core.types.infer.TypeFoldable
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor

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

val TypeFoldable<*>.hasTyInfer get() = visitWith { it is TyInfer }

val TypeFoldable<*>.needsSubst get(): Boolean = visitWith { it is TyTypeParameter }

interface Ty : TypeFoldable<Ty> {

    override fun foldWith(folder: TypeFolder): Ty = folder(this)

    override fun innerFoldWith(folder: TypeFolder): Ty = this

    override fun visitWith(visitor: TypeVisitor): Boolean = visitor(this)

    override fun innerVisitWith(visitor: TypeVisitor): Boolean = false

    /**
     * Bindings between formal type parameters and actual type arguments.
     */
//    val tyInferValues: Substitution get() = emptySubstitution

    /**
     * User visible string representation of a type
     */
    override fun toString(): String

    fun abilities(): Set<Ability>
}

val Ty.isTypeParam: Boolean get() = this is TyInfer || this is TyTypeParameter

fun Ty.mslTy(): Ty = if (this is TyReference && this.msl) this.innermostTy() else this
