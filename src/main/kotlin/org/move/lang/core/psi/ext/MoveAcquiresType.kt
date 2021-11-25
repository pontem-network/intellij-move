package org.move.lang.core.psi.ext

import org.move.ide.presentation.name
import org.move.lang.core.psi.MoveAcquiresType
import org.move.lang.core.types.ty.TyStruct

val MoveAcquiresType.typeNames: List<String>?
    get() {
        return pathTypeList
            .map { it.resolvedType() }
            .map { (it as? TyStruct) ?: return null }
            .map { it.name() }
    }
