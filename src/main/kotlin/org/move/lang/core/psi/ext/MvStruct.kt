package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.*
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.stubs.MvStructStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.ItemQualName
import org.move.lang.core.types.ty.Ability
import org.move.stdext.withAdded
import javax.swing.Icon

val MvStruct.fields: List<MvStructField>
    get() = structBlock?.structFieldList.orEmpty()

val MvStruct.fieldsMap: Map<String, MvStructField>
    get() {
        return fields.associateBy { it.identifier.text }
    }

val MvStruct.fieldNames: List<String> get() = fields.map { it.name }

//fun MvStruct.getField(fieldName: String): MvStructField? =
//    this.descendantsOfType<MvStructField>().find { it.name == fieldName }

//val MvStruct.fqName: String
//    get() {
//        val moduleFqName = "${this.module.fqName}::"
//        val name = this.name ?: "<unknown>"
//        return moduleFqName + name
//    }

val MvStruct.module: MvModule
    get() {
        val moduleStub = greenStub?.parentStub as? MvModuleStub
        if (moduleStub != null) {
            return moduleStub.psi
        }
        val moduleBlock = this.parent
        return moduleBlock.parent as MvModule
    }

val MvStruct.abilities: List<MvAbility>
    get() {
        return this.abilitiesList?.abilityList ?: emptyList()
    }

val MvStruct.tyAbilities: Set<Ability> get() = this.abilities.mapNotNull { it.ability }.toSet()

val MvStruct.hasPhantomTypeParameters get() = this.typeParameters.any { it.isPhantom }

fun MvStruct.addAbility(ability: String) {
    if (ability in this.abilities.map { it.text }) return

    val newAbilities = this.abilities.mapNotNull { it.text }.withAdded(ability)
    val newAbilitiesList = project.psiFactory.abilitiesList(newAbilities)
    if (this.abilitiesList != null) {
        this.abilitiesList?.replace(newAbilitiesList)
    } else {
        val anchor = when {
            this.structBlock != null -> this.structBlock
            this.hasChild(MvElementTypes.SEMICOLON) -> this.getChild(MvElementTypes.SEMICOLON)
            else -> return
        }
        this.addBefore(newAbilitiesList, anchor)
    }
}

abstract class MvStructMixin : MvStubbedNamedElementImpl<MvStructStub>,
                               MvStruct {

    constructor(node: ASTNode) : super(node)

    constructor(stub: MvStructStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT

    override val qualName: ItemQualName?
        get() {
            val itemName = this.name ?: return null
            val moduleFQName = this.module.qualName ?: return null
            return ItemQualName(this, moduleFQName.address, moduleFQName.itemName, itemName)
        }
}
