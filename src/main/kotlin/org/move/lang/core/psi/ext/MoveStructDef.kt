package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.util.descendantsOfType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveStructFieldDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import javax.swing.Icon

val MoveStructDef.fields: List<MoveStructFieldDef>
    get() = structFieldsDefBlock?.structFieldDefList.orEmpty()

val MoveStructDef.fieldsMap: Map<String, MoveStructFieldDef>
    get() {
        return fields.associateBy { it.identifier.text }
    }

val MoveStructDef.fieldNames: List<String>
    get() = fields.mapNotNull { it.name }

fun MoveStructDef.getField(fieldName: String): MoveStructFieldDef? =
    this.descendantsOfType<MoveStructFieldDef>()
        .find { it.name == fieldName }

abstract class MoveStructDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveStructDef {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT
}
