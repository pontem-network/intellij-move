package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvSchemaFieldStatement
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

abstract class MvSchemaFieldStatementMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                            MvSchemaFieldStatement {

}
