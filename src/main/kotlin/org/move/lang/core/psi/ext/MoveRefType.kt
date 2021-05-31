package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveRefType
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.RefType

val MoveRefType.mutable: Boolean
    get() =
        "mut" in this.refTypeStart.text


abstract class MoveRefTypeMixin(node: ASTNode) : MoveElementImpl(node), MoveRefType {
    override fun resolvedType(): BaseType? {
        return this
            .type?.resolvedType()?.let { RefType(it, this.mutable) }
    }

}
