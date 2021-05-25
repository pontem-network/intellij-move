package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.RefType
import org.move.lang.core.types.UnresolvedType

val MoveType.resolvedType: BaseType
    get() {
        return when (this) {
            is MoveQualPathType -> {
                val referred = this.reference?.resolve() ?: return UnresolvedType()
                when (referred) {
                    is MoveTypeParameter -> referred.typeParamType
                    is MoveStructSignature -> referred.structDef?.structType ?: UnresolvedType()
                    else -> UnresolvedType()
                }
            }
            is MoveRefType -> {
                val referredType = this.type?.resolvedType ?: return UnresolvedType()
                RefType(referredType)
            }
            else -> UnresolvedType()
        }
    }
