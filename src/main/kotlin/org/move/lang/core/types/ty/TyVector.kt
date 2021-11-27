package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString

import org.move.lang.core.types.infer.TypeFolder

open class TyVector(val item: Ty): Ty {
    override fun abilities() = item.abilities()

    override fun innerFoldWith(folder: TypeFolder): Ty =
        TyVector(item.foldWith(folder))

    override fun toString(): String = tyToString(this)

    override fun equals(other: Any?): Boolean = other is TyVector && item == other.item

    override fun hashCode(): Int = item.hashCode()
}

object TyByteString: TyVector(TyInteger(TyInteger.Kind.u8))
