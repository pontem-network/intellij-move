package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveStructLiteralExpr
import org.move.lang.core.psi.MoveStructLiteralField
import org.move.lang.core.psi.typeParameters
import org.move.lang.core.types.infer.Constraint
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.foldTyTypeParameterWith
import org.move.lang.core.types.ty.*

val MoveStructLiteralExpr.providedFields: List<MoveStructLiteralField>
    get() =
        structLiteralFieldsBlock.structLiteralFieldList

val MoveStructLiteralExpr.providedFieldNames: List<String>
    get() =
        providedFields.map { it.referenceName }

abstract class MoveStructLiteralExprMixin(node: ASTNode) : MoveElementImpl(node), MoveStructLiteralExpr {
    override fun resolvedType(): Ty {
        val structItem = this.path.maybeStructSignature ?: return TyUnknown
        val structTypeVars = structItem.typeParameters.map { TyInfer.TyVar(TyTypeParameter(it)) }

        val inference = InferenceContext()
        // TODO: combine it with TyStruct constructor
        val typeArgs = this.path.typeArguments
        if (typeArgs.isNotEmpty()) {
            if (typeArgs.size < structItem.typeParameters.size) return TyUnknown
            for ((tyVar, typeArg) in structTypeVars.zip(typeArgs)) {
                inference.registerConstraint(Constraint.Equate(tyVar, typeArg.type.resolvedType()))
            }
        }
        for (field in this.providedFields) {
            val fieldName = field.referenceName
            val declaredFieldTy = structItem.structDef
                .fieldsMap[fieldName]
                ?.resolvedType()
                ?.foldTyTypeParameterWith { param -> structTypeVars.find { it.origin?.parameter == param.parameter }!! }
                ?: TyUnknown
            val fieldExprTy = field.assignedExprTy
            inference.registerConstraint(Constraint.Equate(declaredFieldTy, fieldExprTy))
        }
        inference.processConstraints()

        val structTy = TyStruct(structItem, structTypeVars)
        return inference.resolveTy(structTy)
    }
}
