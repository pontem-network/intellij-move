package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvAbility
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.types.ty.TyTypeParameter

val MvTypeParameter.isPhantom get() = hasChild(MvElementTypes.PHANTOM)

val MvTypeParameter.typeParamType: TyTypeParameter
    get() {
        return TyTypeParameter(this)
    }

val MvTypeParameter.abilities: List<MvAbility>
    get() {
        return typeParamBound?.abilityList.orEmpty()
    }
