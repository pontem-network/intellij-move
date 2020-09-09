package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveImport
import org.move.lang.core.psi.MoveImportAlias
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveImportAliasMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                     MoveImportAlias {
}