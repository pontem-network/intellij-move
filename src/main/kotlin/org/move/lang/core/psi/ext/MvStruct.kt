package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.util.descendantsOfType
import org.move.ide.MvIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import javax.swing.Icon

val MvStruct.fields: List<MvStructField>
    get() = structBlock?.structFieldList.orEmpty()

val MvStruct.fieldsMap: Map<String, MvStructField>
    get() {
        return fields.associateBy { it.identifier.text }
    }

val MvStruct.fieldNames: List<String>
    get() = fields.mapNotNull { it.name }

fun MvStruct.getField(fieldName: String): MvStructField? =
    this.descendantsOfType<MvStructField>()
        .find { it.name == fieldName }

val MvStruct.fqName: String get() {
    val moduleFqName = this.module.fqName?.let { "$it::" }
    val name = this.name ?: "<unknown>"
    return moduleFqName + name
}

val MvStruct.module: MvModuleDef
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as MvModuleDef
    }

val MvStruct.abilities: List<MvAbility>
    get() {
        return this.abilitiesList?.abilityList ?: emptyList()
    }

val MvStruct.hasPhantomTypeParameters get() = this.typeParameters.any { it.isPhantom }

abstract class MvStructMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                              MvStruct {

    override fun getIcon(flags: Int): Icon = MvIcons.STRUCT
}
