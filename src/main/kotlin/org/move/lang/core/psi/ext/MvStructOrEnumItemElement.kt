package org.move.lang.core.psi.ext

import com.intellij.psi.StubBasedPsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.types.ty.Ability
import org.move.lang.core.types.ty.TyAdt

interface MvStructOrEnumItemElement: MvQualNamedElement,
                                     MvItemElement,
                                     MvTypeParametersOwner {

    val abilitiesList: MvAbilitiesList?

    override fun declaredType(msl: Boolean): TyAdt = TyAdt(this, this.tyTypeParams, this.generics)
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

val MvStructOrEnumItemElement.module: MvModule
    get() {
        if (this is StubBasedPsiElement<*>) {
            val moduleStub = greenStub?.parentStub as? MvModuleStub
            if (moduleStub != null) {
                return moduleStub.psi
            }
        }
        return this.parent as MvModule
    }
