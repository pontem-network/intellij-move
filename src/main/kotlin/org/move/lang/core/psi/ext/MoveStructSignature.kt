package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

val MoveStructSignature.fqName: String get() {
    val moduleFqName = this.module.fqName?.let { "$it::" }
    val name = this.name ?: "<unknown>"
    return moduleFqName + name
}

val MoveStructSignature.module: MoveModuleDef
    get() {
        val moduleBlock = this.parent.parent
        return moduleBlock.parent as MoveModuleDef
    }

val MoveStructSignature.structDef: MoveStructDef
    get() {
        return this.parent as MoveStructDef
    }

val MoveStructSignature.abilities: List<MoveAbility>
    get() {
        return this.abilitiesList?.abilityList ?: emptyList()
    }
