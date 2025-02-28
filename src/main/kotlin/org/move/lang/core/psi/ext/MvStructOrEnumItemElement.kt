package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvAbilitiesList
import org.move.lang.core.psi.MvAbility
import org.move.lang.core.psi.MvGenericDeclaration
import org.move.lang.core.psi.MvModule
import org.move.lang.core.types.ty.Ability

interface MvStructOrEnumItemElement: MvItemElement,
                                     MvTypeDeclarationElement,
                                     MvGenericDeclaration {

    val abilitiesList: MvAbilitiesList?

    // it's public except for the lit/pat usages, which are checked separately
    override val isPublic: Boolean get() = true
}

val MvStructOrEnumItemElement.psiAbilities: List<MvAbility>
    get() {
        return this.abilitiesList?.abilityList ?: emptyList()
    }

val MvStructOrEnumItemElement.abilities: Set<Ability>
    get() = this.psiAbilities.mapNotNull { it.ability }.toSet()

val MvStructOrEnumItemElement.hasKey: Boolean get() = Ability.KEY in abilities
val MvStructOrEnumItemElement.hasStore: Boolean get() = Ability.STORE in abilities
val MvStructOrEnumItemElement.hasCopy: Boolean get() = Ability.COPY in abilities
val MvStructOrEnumItemElement.hasDrop: Boolean get() = Ability.DROP in abilities

val MvStructOrEnumItemElement.module: MvModule get() = this.parent as MvModule
