package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveQualTypeReferenceElement
import org.move.lang.core.psi.MoveRefType
import org.move.lang.core.psi.MoveType
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.RefType
import org.move.lang.core.types.UnresolvedType

val MoveType.resolvedType: BaseType
    get() {
        return when (this) {
            is MoveQualTypeReferenceElement -> {
                this.referredStructDef?.structType ?: UnresolvedType()
            }
            is MoveRefType -> {
                val referredType = this.type?.resolvedType ?: return UnresolvedType()
                RefType(referredType)
            }
            else -> UnresolvedType()
        }
    }
