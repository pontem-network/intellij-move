package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvAbilitiesList
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.ty.Ability
import org.move.stdext.withAdded
import javax.swing.Icon

val MvStruct.requiredAbilitiesForTypeParam: Set<Ability>
    get() =
        this.abilities.map { it.requires() }.toSet()

val MvStruct.hasPhantomTypeParameters get() = this.typeParameters.any { it.isPhantom }

fun MvStruct.addAbility(ability: String) {
    if (ability in this.psiAbilities.map { it.text }) return

    val newAbilities = this.psiAbilities.mapNotNull { it.text }.withAdded(ability)
    val newAbilitiesList = project.psiFactory.abilitiesList(newAbilities)
    if (this.abilitiesList != null) {
        this.abilitiesList?.replace(newAbilitiesList)
    } else {
        val anchor = when {
            this.blockFields != null -> this.blockFields
            this.hasChild(MvElementTypes.SEMICOLON) -> this.getChild(MvElementTypes.SEMICOLON)
            else -> return
        }
        this.addBefore(newAbilitiesList, anchor)
    }
}

abstract class MvStructMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                             MvStruct {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT

    override val abilitiesList: MvAbilitiesList? get() = abilitiesListList.firstOrNull()
}
