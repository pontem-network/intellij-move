package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvSpecSchema
import org.move.lang.core.psi.MvTypeParameter

val MvSpecSchema.typeParams: List<MvTypeParameter>
    get() = typeParameterList?.typeParameterList.orEmpty()
