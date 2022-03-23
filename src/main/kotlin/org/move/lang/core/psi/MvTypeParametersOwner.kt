package org.move.lang.core.psi

interface MvTypeParametersOwner : MvElement {
    val typeParameterList: MvTypeParameterList?
}

val MvTypeParametersOwner.typeParameters: List<MvTypeParameter>
    get() =
        typeParameterList?.typeParameterList.orEmpty()

val MvTypeParametersOwner.hasTypeParameters: Boolean
    get() =
        typeParameters.isNotEmpty()
