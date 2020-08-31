package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveStructPatField
import org.move.lang.core.psi.impl.MoveNamedIdentifierOwnerImpl

abstract class MoveStructPatFieldImplMixin(node: ASTNode) : MoveNamedIdentifierOwnerImpl(node),
                                                            MoveStructPatField {

}