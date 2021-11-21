package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MoveStructSignature

class TyStruct(val item: MoveStructSignature, val typeArguments: List<Ty> = emptyList()) : Ty {
    override fun toString(): String = tyToString(this)
}
