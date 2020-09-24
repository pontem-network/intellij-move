package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualPathType
import org.move.lang.core.psi.MoveQualTypeReferenceElementImpl

abstract class MoveQualPathTypeMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                      MoveQualPathType {
}