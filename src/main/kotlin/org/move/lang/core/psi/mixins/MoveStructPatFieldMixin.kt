package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveStructPatField
import org.move.lang.core.psi.impl.MoveNamedIdentifierOwnerImpl

abstract class MoveStructPatFieldMixin(node: ASTNode) : MoveNamedIdentifierOwnerImpl(node),
                                                        MoveStructPatField {

}