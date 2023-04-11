package org.move.lang.core.psi

import org.move.lang.core.types.ty.TyTypeParameter

interface MvTypeParametersOwner : MvElement {
    val typeParameterList: MvTypeParameterList?
}

val MvTypeParametersOwner.typeParameters: List<MvTypeParameter>
    get() =
        typeParameterList?.typeParameterList.orEmpty()

val MvTypeParametersOwner.generics: List<TyTypeParameter>
    get() = typeParameters.map { TyTypeParameter(it) }

val MvTypeParametersOwner.hasTypeParameters: Boolean
    get() =
        typeParameters.isNotEmpty()
