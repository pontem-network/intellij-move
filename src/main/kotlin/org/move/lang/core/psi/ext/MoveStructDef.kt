package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveTypeParameter

val MoveStructDef.typeParams: List<MoveTypeParameter>
    get() = typeParameterList?.typeParameterList.orEmpty()