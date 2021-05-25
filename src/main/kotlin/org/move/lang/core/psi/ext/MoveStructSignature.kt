package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveAbility
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveStructSignature

val MoveStructSignature.module: MoveModuleDef
    get() {
        val moduleBlock = this.parent.parent
        return moduleBlock.parent as MoveModuleDef
    }

val MoveStructSignature.structDef: MoveStructDef?
    get() {
        return this.parent as? MoveStructDef
    }

val MoveStructSignature.abilities: List<MoveAbility>
    get() {
        return this.abilitiesList?.abilityList ?: emptyList()
    }
