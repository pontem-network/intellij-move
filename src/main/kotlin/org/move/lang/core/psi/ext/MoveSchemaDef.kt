package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvSchemaSpecDef
import org.move.lang.core.psi.MvTypeParameter

val MvSchemaSpecDef.typeParams: List<MvTypeParameter>
    get() = typeParameterList?.typeParameterList.orEmpty()
