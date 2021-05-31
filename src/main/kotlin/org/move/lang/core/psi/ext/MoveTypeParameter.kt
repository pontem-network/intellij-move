package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveAbility
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.types.TypeParamType

//val MoveTypeParameter.typeParamType: TypeParamType
//    get() {
//        return TypeParamType(this)
//    }

val MoveTypeParameter.abilities: List<MoveAbility>
    get() {
        return typeParamBound?.abilityList.orEmpty()
    }
