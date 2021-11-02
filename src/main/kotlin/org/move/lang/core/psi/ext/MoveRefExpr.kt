package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.HasType
import org.move.lang.core.types.TypeVarsMap

abstract class MoveRefExprMixin(node: ASTNode) : MoveElementImpl(node), MoveRefExpr {

    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        val referred = this.path.reference?.resolve() ?: return null
        if (referred !is HasType) return null

        return referred.resolvedType(emptyMap())
    }
}
