/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString

import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor

enum class Mutability {
    MUTABLE,
    IMMUTABLE;

    val isMut: Boolean get() = this == MUTABLE

    companion object {
        fun valueOf(mutable: Boolean): Mutability =
            if (mutable) MUTABLE else IMMUTABLE
    }
}

data class TyReference(val referenced: Ty, val mutability: Mutability) : Ty {
    override fun abilities() = setOf(Ability.COPY, Ability.DROP)

    fun innerTy(): Ty {
        if (referenced is TyReference) {
            return referenced.innerTy()
        } else {
            return referenced
        }
    }

    fun innermostTy(): Ty {
        var ty: Ty = this
        while (ty is TyReference) {
            ty = ty.innerTy()
        }
        return ty
    }

    override fun innerFoldWith(folder: TypeFolder): Ty =
        TyReference(referenced.foldWith(folder), mutability)

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        referenced.visitWith(visitor)

    override fun toString(): String = tyToString(this)
}
