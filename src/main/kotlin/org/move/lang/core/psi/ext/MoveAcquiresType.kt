package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveAcquiresType
import org.move.lang.core.types.StructType

val MoveAcquiresType.typeNames: List<String>?
    get() {
        return qualPathTypeList
            .map { it.resolvedType(emptyMap()) ?: return null }
            .map { (it as? StructType) ?: return null }
            .map { it.name() }
    }
