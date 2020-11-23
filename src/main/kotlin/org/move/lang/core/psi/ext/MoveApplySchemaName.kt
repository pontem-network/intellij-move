package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveApplySchemaName
import org.move.lang.core.psi.MoveQualSchemaReferenceElementImpl

abstract class MoveApplySchemaNameMixin(node: ASTNode) : MoveQualSchemaReferenceElementImpl(node),
                                                         MoveApplySchemaName {
    override val isPrimitive: Boolean
        get() = false
}