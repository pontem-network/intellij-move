package org.move.lang.core.psi.ext

import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.MoveFunctionParameter

val MoveFunctionDef.params: List<MoveFunctionParameter>
    get() =
        this.functionParameterList?.functionParameterList.orEmpty()

val MoveFunctionDef.isPublic: Boolean
    get() = isChildExists(MoveElementTypes.PUBLIC)