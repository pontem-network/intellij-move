package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveStructPatField
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl

abstract class MoveStructPatFieldMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                        MoveStructPatField {

}