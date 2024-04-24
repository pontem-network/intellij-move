/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.infer.foldTyTypeParameterWith
import org.move.lang.core.types.infer.isCompatible
import org.move.lang.core.types.ty.RefPermissions.READ
import org.move.lang.core.types.ty.RefPermissions.WRITE

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
): Ty(referenced.flags) {
    override fun abilities() = setOf(Ability.COPY, Ability.DROP)

    val isMut: Boolean get() = this.permissions.contains(WRITE)

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
        fun ref(ty: Ty, mut: Boolean, msl: Boolean = false): TyReference =
            TyReference(ty, if (mut) setOf(READ, WRITE) else setOf(READ), msl)

        fun coerceMutability(inferred: TyReference, expected: TyReference): Boolean {
            return inferred.isMut || !expected.isMut
        }

        fun isCompatibleWithAutoborrow(ty: Ty, intoTy: Ty, msl: Boolean): Boolean {
            // if underlying types are different, no match
            val autoborrowedTy = autoborrow(ty, intoTy) ?: return false
            return isCompatible(intoTy, autoborrowedTy, msl)
        }

        fun autoborrow(ty: Ty, intoTy: Ty): Ty? {
            return if (intoTy is TyReference) coerceAutoborrow(ty, intoTy.isMut) else ty
        }

        fun coerceAutoborrow(ty: Ty, mut: Boolean): Ty? {
            return when {
                ty !is TyReference -> ref(ty, mut)
                mut && ty.isMut -> ty
                mut && !ty.isMut -> null
                !mut && ty.isMut -> ref(ty.innerTy(), false)
                !mut && !ty.isMut -> ty
                else -> null
            }
        }
    }
}
