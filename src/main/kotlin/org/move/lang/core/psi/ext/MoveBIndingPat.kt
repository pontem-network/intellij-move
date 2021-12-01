package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveBindingPat
import org.move.lang.core.psi.MoveLetStatement
import org.move.lang.core.psi.impl.MoveNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.collectBindings
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.infer.inferMoveTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MoveBindingPat.letStmt: MoveLetStatement get() = this.ancestorStrict()!!

abstract class MoveBindingPatMixin(node: ASTNode) : MoveNameIdentifierOwnerImpl(node),
                                                    MoveBindingPat {

    override fun getIcon(flags: Int): Icon = MoveIcons.VARIABLE

    override fun resolvedType(): Ty {
        val pat = letStmt.pat ?: return TyUnknown
        val explicitType = this.letStmt.typeAnnotation?.type
        if (explicitType != null) {
            val explicitTy = inferMoveTypeTy(explicitType)
            return collectBindings(pat, explicitTy)[this] ?: TyUnknown
        }

        val inference = InferenceContext()
        val inferredTy = letStmt.initializer?.expr?.let { inferExprTy(it, inference) } ?: TyUnknown
        return collectBindings(pat, inferredTy)[this] ?: TyUnknown
    }
}
