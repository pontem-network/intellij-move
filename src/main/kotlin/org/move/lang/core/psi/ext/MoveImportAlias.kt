package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvUseAlias
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

abstract class MvUseAliasMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                MvUseAlias {
}
