package org.move.lang.core.psi.ext

import com.intellij.psi.util.descendantsOfType
import org.move.lang.core.psi.*

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
