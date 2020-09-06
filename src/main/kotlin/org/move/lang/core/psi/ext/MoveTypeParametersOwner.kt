package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveElement
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.psi.MoveTypeParameterList

interface MoveTypeParametersOwner : MoveElement {
    val typeParameterList: MoveTypeParameterList?
}

val MoveTypeParametersOwner.typeParams: List<MoveTypeParameter>
    get() = typeParameterList?.typeParameterList.orEmpty()
