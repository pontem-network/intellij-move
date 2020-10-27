package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveStructFieldDef
import org.move.lang.core.psi.MoveTypeParameter
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import javax.swing.Icon

val MoveStructDef.fields: List<MoveStructFieldDef>
    get() = structFieldsDefBlock?.structFieldDefList.orEmpty()

val MoveStructDef.fieldNames: List<String>
    get() = fields.mapNotNull { it.name }

val MoveStructDef.typeParams: List<MoveTypeParameter>
    get() = typeParameterList?.typeParameterList.orEmpty()


abstract class MoveStructDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                   MoveStructDef {

    override fun getIcon(flags: Int): Icon = MoveIcons.STRUCT
}