package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveApplySchemaName
import org.move.lang.core.psi.impl.MoveSchemaReferenceElementImpl

abstract class MoveApplySchemaNameMixin(node: ASTNode) : MoveSchemaReferenceElementImpl(node),
                                                         MoveApplySchemaName {
    override val referenceNameElement: PsiElement
        get() = qualifiedPath.identifier
}