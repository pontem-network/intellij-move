package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveAbility
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.types.TypeParamType
import org.move.lang.core.types.ty.TyTypeParameter

val MoveTypeParameter.typeParamType: TyTypeParameter
    get() {
        return TyTypeParameter(this)
    }

val MoveTypeParameter.abilities: List<MoveAbility>
    get() {
        return typeParamBound?.abilityList.orEmpty()
    }
