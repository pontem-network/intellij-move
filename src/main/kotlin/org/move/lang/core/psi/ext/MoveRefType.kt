package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveRefType
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.RefType
import org.move.lang.core.types.TypeVarsMap

val MoveRefType.mutable: Boolean
    get() =
        "mut" in this.refTypeStart.text


abstract class MoveRefTypeMixin(node: ASTNode) : MoveElementImpl(node), MoveRefType {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        val innerType = this.type ?: return null
        val referredType = innerType.resolvedType(typeVars) ?: return null
        return RefType(referredType, this.mutable)
    }

}
