package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.MoveTypeParameter

val MoveFunctionDef.typeParams: List<MoveTypeParameter>
    get() = typeParameterList?.typeParameterList.orEmpty()