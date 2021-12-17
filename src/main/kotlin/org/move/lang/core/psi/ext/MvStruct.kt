package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.util.descendantsOfType
import org.move.ide.MvIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import javax.swing.Icon

val MvStruct_.fields: List<MvStructFieldDef>
    get() = structFieldsDefBlock?.structFieldDefList.orEmpty()

val MvStruct_.fieldsMap: Map<String, MvStructFieldDef>
    get() {
        return fields.associateBy { it.identifier.text }
    }

val MvStruct_.fieldNames: List<String>
    get() = fields.mapNotNull { it.name }

fun MvStruct_.getField(fieldName: String): MvStructFieldDef? =
    this.descendantsOfType<MvStructFieldDef>()
        .find { it.name == fieldName }

val MvStruct_.fqName: String get() {
    val moduleFqName = this.module.fqName?.let { "$it::" }
    val name = this.name ?: "<unknown>"
    return moduleFqName + name
}

val MvStruct_.module: MvModuleDef
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as MvModuleDef
    }

val MvStruct_.abilities: List<MvAbility>
    get() {
        return this.abilitiesList?.abilityList ?: emptyList()
    }

abstract class MvStructMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                              MvStruct_ {

    override fun getIcon(flags: Int): Icon = MvIcons.STRUCT
}
