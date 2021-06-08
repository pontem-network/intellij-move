package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveSchemaSpecDef
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveTypeParameter

val MoveSchemaSpecDef.typeParams: List<MoveTypeParameter>
    get() = typeParameterList?.typeParameterList.orEmpty()
