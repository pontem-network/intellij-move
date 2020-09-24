package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.ref_element.MoveQualTypeReferenceElementImpl
import org.move.lang.core.psi.MoveQualPathType

abstract class MoveQualPathTypeMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                      MoveQualPathType {
}