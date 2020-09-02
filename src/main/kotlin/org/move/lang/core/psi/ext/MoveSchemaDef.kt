package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveSchemaDef
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveTypeParameter

val MoveSchemaDef.typeParams: List<MoveTypeParameter>
    get() = typeParameterList?.typeParameterList.orEmpty()