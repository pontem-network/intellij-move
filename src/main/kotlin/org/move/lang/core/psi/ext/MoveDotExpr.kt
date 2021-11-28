package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveDotExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.types.infer.Constraint
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown

val MoveDotExpr.refExpr: MoveRefExpr?
    get() {
        return this.expr as? MoveRefExpr
    }

abstract class MoveDotExprMixin(node: ASTNode) : MoveElementImpl(node), MoveDotExpr {
    override fun resolvedType(): Ty {
        val objectTy = this.expr.resolvedType()
        val structTy =
            when (objectTy) {
                is TyReference -> objectTy.referenced as? TyStruct
                is TyStruct -> objectTy
                else -> null
            } ?: return TyUnknown

        val inferenceContext = InferenceContext()
        for ((tyVar, tyArg) in structTy.typeVars.zip(structTy.typeArguments)) {
            inferenceContext.registerConstraint(Constraint.Equate(tyVar, tyArg))
        }
        inferenceContext.processConstraints()

        val fieldName = this.structFieldRef.referenceName
        return inferenceContext.resolveTy(structTy.fieldTy(fieldName))
    }
}
