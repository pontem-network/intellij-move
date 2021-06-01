package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualNameReferenceElementImpl
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.HasType

abstract class MoveRefExprMixin(node: ASTNode) : MoveQualNameReferenceElementImpl(node),
                                                 MoveRefExpr {
    override fun resolvedType(): BaseType? {
        val referred = this.reference.resolve() ?: return null
        if (referred !is HasType) return null

        return referred.resolvedType()
    }
}
