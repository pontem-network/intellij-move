package org.move.lang.core.psi

import org.move.lang.core.types.infer.Substitution
import org.move.lang.core.types.infer.toTypeSubst
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyTypeParameter

interface MvGenericDeclaration: MvElement {
    val typeParameterList: MvTypeParameterList?
}

val MvGenericDeclaration.typeParameters: List<MvTypeParameter>
    get() =
        typeParameterList?.typeParameterList.orEmpty()

val MvGenericDeclaration.generics: List<TyTypeParameter>
    get() = typeParameters.map { TyTypeParameter.named(it) }

val MvGenericDeclaration.tyTypeParams: Substitution get() = Substitution(generics.associateWith { it })

val MvGenericDeclaration.tyInfers: Substitution
    get() {
        val typeSubst = this
            .generics
            .associateWith { TyInfer.TyVar(it) }
        return typeSubst.toTypeSubst()
    }

val MvGenericDeclaration.hasTypeParameters: Boolean
    get() =
        typeParameters.isNotEmpty()
