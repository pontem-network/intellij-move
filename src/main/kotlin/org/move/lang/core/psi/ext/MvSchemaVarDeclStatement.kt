package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvSchemaVarDeclStatement
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

abstract class MvSchemaVarDeclStatementMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                              MvSchemaVarDeclStatement {

}
