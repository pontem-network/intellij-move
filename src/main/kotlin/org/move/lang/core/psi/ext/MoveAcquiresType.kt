package org.move.lang.core.psi.ext

import org.move.ide.presentation.name
import org.move.lang.core.psi.MvAcquiresType
import org.move.lang.core.types.ty.TyStruct

val MvAcquiresType.typeFQNames: List<String>?
    get() {
        return pathTypeList
            .map { it.inferTypeTy() }
            .map { (it as? TyStruct) ?: return null }
            .map { it.item.fqName }
    }
