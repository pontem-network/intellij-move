package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveConstDef
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.TypeVarsMap
import javax.swing.Icon

abstract class MoveConstDefMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                  MoveConstDef {

    override fun getIcon(flags: Int): Icon = MoveIcons.CONST

    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        return this.typeAnnotation?.type?.resolvedType(emptyMap())
    }
}
