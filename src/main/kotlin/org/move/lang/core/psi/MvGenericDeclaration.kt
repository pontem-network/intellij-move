package org.move.lang.core.psi

import org.move.lang.core.types.infer.Substitution
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyTypeParameter

interface MvGenericDeclaration: MvElement {
    val typeParameterList: MvTypeParameterList?
}

val MvGenericDeclaration.typeParameters: List<MvTypeParameter>
    get() =
        typeParameterList?.typeParameterList.orEmpty()

val MvGenericDeclaration.tyTypeParams: List<TyTypeParameter>
    get() = typeParameters.map { TyTypeParameter.named(it) }

val MvGenericDeclaration.typeParamsToTypeParamsSubst: Substitution get() =
    Substitution(tyTypeParams.associateWith { it })

val MvGenericDeclaration.typeParamsToTyVarsSubst: Substitution
    get() {
        val typeSubst = this.tyTypeParams.associateWith { TyInfer.TyVar(it) }
        return Substitution(typeSubst)
    }

val MvGenericDeclaration.hasTypeParameters: Boolean
    get() =
        typeParameters.isNotEmpty()
