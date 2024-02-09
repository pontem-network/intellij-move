/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString

import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor

enum class RefPermissions {
    READ,
    WRITE;

    companion object {
        fun valueOf(mutable: Boolean): Set<RefPermissions> =
            if (mutable) setOf(READ, WRITE) else setOf(READ)
    }
}

data class TyReference(
    val referenced: Ty,
    val permissions: Set<RefPermissions>,
    val msl: Boolean
) : Ty(referenced.flags) {
    override fun abilities() = setOf(Ability.COPY, Ability.DROP)

    val isMut: Boolean get() = this.permissions.contains(RefPermissions.WRITE)

    fun innerTy(): Ty {
        return if (referenced is TyReference) {
            referenced.innerTy()
        } else {
            referenced
        }
    }

    fun innermostTy(): Ty {
        var ty: Ty = this
        while (ty is TyReference) {
            ty = ty.innerTy()
        }
        return ty
    }

    fun transferReference(otherTy: Ty): Ty = TyReference(otherTy, this.permissions, this.msl)

    override fun innerFoldWith(folder: TypeFolder): Ty =
        TyReference(referenced.foldWith(folder), permissions, msl)

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        referenced.visitWith(visitor)

    override fun toString(): String = tyToString(this)

    companion object {
        fun ref(ty: Ty, msl: Boolean): TyReference = TyReference(ty, setOf(RefPermissions.READ), msl)

        fun coerceMutability(inferred: TyReference, expected: TyReference): Boolean {
            return inferred.isMut || !expected.isMut
        }
    }
}
