package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveDotExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.types.BaseType
import org.move.lang.core.types.RefType
import org.move.lang.core.types.StructType
import org.move.lang.core.types.TypeVarsMap

val MoveDotExpr.refExpr: MoveRefExpr?
    get() {
        return this.expr as? MoveRefExpr
    }

abstract class MoveDotExprMixin(node: ASTNode) : MoveElementImpl(node), MoveDotExpr {
    override fun resolvedType(typeVars: TypeVarsMap): BaseType? {
        val objectType = this.expr.resolvedType(typeVars)
        val structType =
            when (objectType) {
                is RefType -> objectType.innerReferredType() as? StructType
                is StructType -> objectType
                else -> null
            }
        if (structType == null) return null

        val fieldName = this.structFieldRef.referenceName ?: return null
        val field = structType.structDef()?.fieldsMap?.get(fieldName) ?: return null

        val fieldTypeVars = structType.typeVars()
        return field.typeAnnotation?.type?.resolvedType(fieldTypeVars)
    }
}
