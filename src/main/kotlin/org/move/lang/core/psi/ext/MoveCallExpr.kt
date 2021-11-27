package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.*
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyUnknown

val MoveCallExpr.typeArguments: List<MoveTypeArgument>
    get() {
        return this.path.typeArguments
    }

val MoveCallExpr.arguments: List<MoveExpr>
    get() {
        return this.callArguments?.exprList.orEmpty()
    }

abstract class MoveCallExprMixin(node: ASTNode) : MoveElementImpl(node), MoveCallExpr {

    override fun resolvedType(): Ty {
        val inference = InferenceContext()

        val path = this.path
        val funcItem = path.reference?.resolve() as? MoveFunctionSignature ?: return TyUnknown
        val funcTy = instantiateItemTy(funcItem) as? TyFunction ?: return TyUnknown

        // find all types passed as explicit type parameters, create constraints with those
        if (path.typeArguments.isNotEmpty()) {
            if (path.typeArguments.size != funcTy.typeVars.size) return TyUnknown
            for ((typeVar, typeArgument) in funcTy.typeVars.zip(path.typeArguments)) {
                // TODO: resolve passedTy with current context
                val passedTy = inferMoveTypeTy(typeArgument.type)
                inference.registerConstraint(Constraint.Equate(typeVar, passedTy))
            }
        }
        // find all types of passed expressions, create constraints with those
        if (this.arguments.isNotEmpty()) {
            for ((paramTy, argumentExpr) in funcTy.paramTypes.zip(this.arguments)) {
                val argumentTy = argumentExpr.resolvedType()
//                val argumentTy = this.inferExprTy(argumentExpr)
                inference.registerConstraint(Constraint.Equate(paramTy, argumentTy))
            }
        }
        // solve constraints
        inference.processConstraints()
        // see whether every arg is coerceable with those vars having those values
        // resolve return type with those vars
        return inference.resolveTy(funcTy.retType)
    }
}
