package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveFunctionParams
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.impl.MoveElementImpl

abstract class MoveFunctionParamsMixin(node: ASTNode) : MoveElementImpl(node),
                                                        MoveFunctionParams {
    override val boundElements: Collection<MoveNamedElement>
        get() = functionParamList
}