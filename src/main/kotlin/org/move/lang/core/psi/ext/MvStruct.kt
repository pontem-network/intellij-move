package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.stubs.MvStructStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.MvPsiTypeImplUtil
import org.move.lang.core.types.ty.Ability
import org.move.lang.core.types.ty.Ty
import org.move.stdext.withAdded
import javax.swing.Icon

//val MvStruct.fields: List<MvNamedFieldDecl>
//    get() = blockFields?.namedFieldDeclList.orEmpty()

//val MvStruct.fieldsMap: Map<String, MvNamedFieldDecl>
//    get() {
//        return fields.associateBy { it.name }
//    }

//val MvStruct.fieldNames: List<String> get() = fields.map { it.name }

//fun MvStruct.getField(fieldName: String): MvStructField? =
//    this.descendantsOfType<MvStructField>().find { it.name == fieldName }

//val MvStruct.fqName: String
//    get() {
//        val moduleFqName = "${this.module.fqName}::"
//        val name = this.name ?: "<unknown>"
//        return moduleFqName + name
//    }

//val MvStruct.module: MvModule
//    get() {
//        val moduleStub = greenStub?.parentStub as? MvModuleStub
//        if (moduleStub != null) {
//            return moduleStub.psi
//        }
//        return this.parent as MvModule
//    }

//val MvStruct.psiAbilities: List<MvAbility>
//    get() {
//        return this.abilitiesList?.abilityList ?: emptyList()
//    }

//val MvStruct.abilities: Set<Ability> get() = this.psiAbilities.mapNotNull { it.ability }.toSet()

//val MvStruct.hasKey: Boolean get() = Ability.KEY in abilities
//val MvStruct.hasStore: Boolean get() = Ability.STORE in abilities
//val MvStruct.hasCopy: Boolean get() = Ability.COPY in abilities
//val MvStruct.hasDrop: Boolean get() = Ability.DROP in abilities

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

abstract class MvStructMixin: MvStubbedNamedElementImpl<MvStructStub>,
                              MvStruct {

    constructor(node: ASTNode): super(node)

    constructor(stub: MvStructStub, nodeType: IStubElementType<*, *>): super(stub, nodeType)

    override fun declaredType(msl: Boolean): Ty = MvPsiTypeImplUtil.declaredType(this)

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT

    override val abilitiesList: MvAbilitiesList? get() = abilitiesListList.firstOrNull()
}
