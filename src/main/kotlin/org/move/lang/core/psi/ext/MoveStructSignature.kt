package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*

val MvStructSignature.fqName: String get() {
    val moduleFqName = this.module.fqName?.let { "$it::" }
    val name = this.name ?: "<unknown>"
    return moduleFqName + name
}

val MvStructSignature.module: MvModuleDef
    get() {
        val moduleBlock = this.parent.parent
        return moduleBlock.parent as MvModuleDef
    }

val MvStructSignature.structDef: MvStructDef
    get() {
        return this.parent as MvStructDef
    }

val MvStructSignature.abilities: List<MvAbility>
    get() {
        return this.abilitiesList?.abilityList ?: emptyList()
    }
