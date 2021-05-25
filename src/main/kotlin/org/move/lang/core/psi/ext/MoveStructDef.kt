package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveStructFieldDef
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.StructType
import javax.swing.Icon

val MoveStructDef.fields: List<MoveStructFieldDef>
    get() = structFieldsDefBlock.structFieldDefList

val MoveStructDef.fieldNames: List<String>
    get() = fields.mapNotNull { it.name }

val MoveStructDef.structType: StructType
    get() = StructType(structSignature)


abstract class MoveStructDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveStructDef {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT
}
