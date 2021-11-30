package org.move.lang.core.types

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveElement
import org.move.lang.core.psi.MoveNameIdentifierOwner
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.types.infer.TypeFoldable
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyTypeParameter

typealias Substitution = Map<TyTypeParameter, Ty>

val emptySubstitution: Substitution = emptyMap()

/**
 * Represents a potentially generic Psi Element, like `fn make_t<T>() { }`,
 * together with actual type arguments, like `T := i32` ([subst]).
 */
class BoundElement<E : MoveNamedElement>(
    val element: E,
    /**
     * Generic type bindings, e.g. if we have some generic declaration
     * `struct S<T>(T)` and its usage with concrete type parameter value
     * `let _: S<u8>;`, [subst] maps `T` to `u8`
     */
    val subst: Substitution = emptySubstitution
) : TypeFoldable<BoundElement<E>> {

    override fun innerFoldWith(folder: TypeFolder): BoundElement<E> =
        BoundElement(
            element,
            subst.mapValues { (_, value) -> value.foldWith(folder) }
        )

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        subst.mapValues { (_, value) -> value.visitWith(visitor) }.any()
}
