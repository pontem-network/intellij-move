package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvElement
import org.move.lang.core.types.ty.Ty

/**
 * Psi element that defines a type and thus lives in types namespace.
 * Archetypal inheritors are structs an enums. Type aliases are type
 * declarations, while constants and statics are not. Notably, traits
 * are type declarations: a bare trait denotes a trait object type.
 *
 */
interface MvTypeDeclarationElement: MvElement {
    fun declaredType(msl: Boolean): Ty
}