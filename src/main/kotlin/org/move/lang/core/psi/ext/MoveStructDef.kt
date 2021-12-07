package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.util.descendantsOfType
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvStructDef
import org.move.lang.core.psi.MvStructFieldDef
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import javax.swing.Icon

val MvStructDef.fields: List<MvStructFieldDef>
    get() = structFieldsDefBlock?.structFieldDefList.orEmpty()

val MvStructDef.fieldsMap: Map<String, MvStructFieldDef>
    get() {
        return fields.associateBy { it.identifier.text }
    }

val MvStructDef.fieldNames: List<String>
    get() = fields.mapNotNull { it.name }

fun MvStructDef.getField(fieldName: String): MvStructFieldDef? =
    this.descendantsOfType<MvStructFieldDef>()
        .find { it.name == fieldName }

abstract class MvStructDefMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                   MvStructDef {

    override fun getIcon(flags: Int): Icon = MvIcons.STRUCT
}
