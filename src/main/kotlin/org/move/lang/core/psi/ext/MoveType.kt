package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveQualTypeReferenceElement
import org.move.lang.core.psi.MoveRefType
import org.move.lang.core.psi.MoveType
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.RefType
import org.move.lang.core.types.StructType
import org.move.lang.core.types.UnresolvedType

val MoveType.resolvedType: BaseType
    get() {
        return when (this) {
            is MoveQualTypeReferenceElement -> {
                val referredStructDef = this.referredStructDef ?: return UnresolvedType()
                StructType(referredStructDef.structSignature)
            }
            is MoveRefType -> {
                val referredType = this.type?.resolvedType ?: return UnresolvedType()
                RefType(referredType)
            }
            else -> UnresolvedType()
        }
    }
