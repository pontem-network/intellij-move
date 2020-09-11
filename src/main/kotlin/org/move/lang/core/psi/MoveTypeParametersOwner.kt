package org.move.lang.core.psi

interface MoveTypeParametersOwner : MoveElement {
    val typeParameterList: MoveTypeParameterList?
}

val MoveTypeParametersOwner.typeParameters: List<MoveTypeParameter>
    get() = typeParameterList?.typeParameterList.orEmpty()