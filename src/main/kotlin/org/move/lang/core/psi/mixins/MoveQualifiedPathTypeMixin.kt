package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveQualifiedPathType
import org.move.lang.core.psi.ext.identifierNameElement
import org.move.lang.core.psi.impl.MoveTypeReferenceElementImpl

abstract class MoveQualifiedPathTypeMixin(node: ASTNode) : MoveTypeReferenceElementImpl(node),
                                                           MoveQualifiedPathType {
    override val referenceNameElement: PsiElement
        get() = qualifiedPath.identifierNameElement
}