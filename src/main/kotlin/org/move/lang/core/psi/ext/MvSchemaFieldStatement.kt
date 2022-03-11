package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvSchemaFieldStmt
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

abstract class MvSchemaFieldStmtMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                            MvSchemaFieldStmt {

}
