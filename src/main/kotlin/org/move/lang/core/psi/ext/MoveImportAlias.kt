package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveImportAlias
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveImportAliasMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                     MoveImportAlias {
}