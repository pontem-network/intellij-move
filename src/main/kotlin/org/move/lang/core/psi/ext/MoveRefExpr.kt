package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveQualNameReferenceElementImpl
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.psi.MoveTypeAnnotated
import org.move.lang.core.psi.type
import org.move.lang.core.types.BaseType

abstract class MoveRefExprMixin(node: ASTNode) : MoveQualNameReferenceElementImpl(node),
                                                 MoveRefExpr {
    override fun resolvedType(): BaseType? {
        val referred = this.reference.resolve() ?: return null
        if (referred !is MoveTypeAnnotated) return null
        return referred.type?.resolvedType()
    }
}
