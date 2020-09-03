package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.MoveFunctionParameter
import org.move.lang.core.psi.MoveTypeParameter

val MoveFunctionDef.params: List<MoveFunctionParameter>
    get() =
        this.functionParameterList?.functionParameterList.orEmpty()
