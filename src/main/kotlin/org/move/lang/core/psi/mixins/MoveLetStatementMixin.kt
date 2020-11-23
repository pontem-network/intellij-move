package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveLetStatement

abstract class MoveLetStatementMixin(node: ASTNode) : MoveElementImpl(node),
                                                      MoveLetStatement 