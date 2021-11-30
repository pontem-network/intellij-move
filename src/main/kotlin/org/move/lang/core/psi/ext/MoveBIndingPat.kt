package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MoveLetStatement
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

abstract class MoveBindingPatMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                    MoveBindingPat {

    override fun getIcon(flags: Int): Icon = MoveIcons.VARIABLE

    override fun resolvedType(): Ty {
        val letStmt = this.parent as? MoveLetStatement ?: return TyUnknown

        val explicitAnnotation = letStmt.typeAnnotation
        val patternType = when {
            explicitAnnotation != null -> explicitAnnotation.type?.resolvedType()
            else -> {
                val initializerExpr = letStmt.initializer?.expr ?: return TyUnknown
                initializerExpr.resolvedType()
            }
        }
        if (patternType == null) return TyUnknown

        return patternType
    }
}
