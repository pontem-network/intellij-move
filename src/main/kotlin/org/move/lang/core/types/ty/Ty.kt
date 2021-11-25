package org.move.lang.core.types.ty

import org.move.lang.core.types.Ability
import org.move.lang.core.types.infer.TypeFoldable
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor

typealias Substitution = Map<TyTypeParameter, Ty>
val emptySubstitution: Substitution = emptyMap()

interface Ty: TypeFoldable<Ty> {

    override fun foldWith(folder: TypeFolder): Ty = folder(this)

    override fun innerFoldWith(folder: TypeFolder): Ty = this

    override fun visitWith(visitor: TypeVisitor): Boolean = visitor(this)

    override fun innerVisitWith(visitor: TypeVisitor): Boolean = false

    /**
     * Bindings between formal type parameters and actual type arguments.
     */
    val typeParameterValues: Substitution get() = emptySubstitution

    /**
     * User visible string representation of a type
     */
    override fun toString(): String

    fun abilities(): Set<Ability>
}
