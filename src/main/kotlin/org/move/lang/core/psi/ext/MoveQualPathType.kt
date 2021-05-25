package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualPathType
import org.move.lang.core.psi.MoveQualTypeReferenceElementImpl
import org.move.lang.core.psi.MoveStructDef
import org.move.lang.core.psi.MoveStructSignature

abstract class MoveQualPathTypeMixin(node: ASTNode) : MoveQualTypeReferenceElementImpl(node),
                                                      MoveQualPathType

val MoveQualPathType.referredStructSignature: MoveStructSignature?
    get() = reference?.resolve() as? MoveStructSignature

val MoveQualPathType.referredStructDef: MoveStructDef?
    get() = referredStructSignature?.structDef
