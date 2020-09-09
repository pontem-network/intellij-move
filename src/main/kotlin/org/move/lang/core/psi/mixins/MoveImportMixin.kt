package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveImport
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveImportMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                MoveImport {
//    override val nameElement: PsiElement?
//        get() = importAlias?.nameElement ?: findChildByType(MoveElementTypes.IDENTIFIER)
}