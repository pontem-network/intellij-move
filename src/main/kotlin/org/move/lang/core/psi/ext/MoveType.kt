package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveQualTypeReferenceElement
import org.move.lang.core.psi.MoveType
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.StructType
import org.move.lang.core.types.UnresolvedType

val MoveType.resolvedType: BaseType
    get() {
        val qualType = this as? MoveQualTypeReferenceElement ?: return UnresolvedType()
        val referredStructDef = qualType.referredStructDef ?: return UnresolvedType()
        return StructType(referredStructDef.structSignature)
    }
