package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvTypeParametersOwner
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyTypeParameter
import org.move.lang.core.types.ty.TyUnknown

/**
 * Represents a potentially generic Psi Element, like `fn make_t<T>() { }`,
 * together with actual type arguments, like `T := i32` ([subst]).
 */
data class BoundElement<out E : MvElement>(
    val element: E,
    /**
     * Generic type bindings, e.g. if we have some generic declaration
     * `struct S<T>(T)` and its usage with concrete type parameter value
     * `let _: S<u8>;`, [subst] maps `T` to `u8`
     */
    val subst: Substitution = emptySubstitution,
) : ResolveResult, TypeFoldable<BoundElement<E>> {

    override fun getElement(): PsiElement = element
    override fun isValidResult(): Boolean = true

    inline fun <reified T : MvElement> downcast(): BoundElement<T>? =
        if (element is T) BoundElement(element, subst) else null

    override fun innerFoldWith(folder: TypeFolder): BoundElement<E> =
        BoundElement(
            element,
            this.subst.foldValues(folder),
        )

    override fun innerVisitWith(visitor: TypeVisitor): Boolean = subst.visitValues(visitor)
}

val BoundElement<MvTypeParametersOwner>.positionalTypeArguments: List<Ty>
    get() = element.typeParameters.map { subst[it] ?: TyTypeParameter(it) }

val BoundElement<MvTypeParametersOwner>.singleParamValue: Ty
    get() = element.typeParameters.singleOrNull()?.let { subst[it] } ?: TyUnknown
