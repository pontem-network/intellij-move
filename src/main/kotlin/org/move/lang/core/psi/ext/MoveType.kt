package org.move.lang.core.psi.ext

import org.move.ide.annotator.BUILTIN_TYPE_IDENTIFIERS
import org.move.ide.annotator.PRIMITIVE_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.PrimitiveType
import org.move.lang.core.types.RefType
import org.move.lang.core.types.UnresolvedType

val MoveType.resolvedType: BaseType
    get() {
        return when (this) {
            is MoveQualPathType -> {
                val referred = this.reference?.resolve()
                if (referred == null) {
                    val refName = this.referenceName ?: return UnresolvedType()
                    return when (refName) {
                        in PRIMITIVE_TYPE_IDENTIFIERS,
                        in BUILTIN_TYPE_IDENTIFIERS -> PrimitiveType(refName)
                        else -> UnresolvedType()
                    }
                }

                when (referred) {
                    is MoveTypeParameter -> referred.typeParamType
                    is MoveStructSignature -> referred.structDef?.structType ?: UnresolvedType()
                    else -> UnresolvedType()
                }
            }
            is MoveRefType -> {
                val referredType = this.type?.resolvedType ?: return UnresolvedType()
                RefType(referredType, this.mutable)
            }
            else -> UnresolvedType()
        }
    }
