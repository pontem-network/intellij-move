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
        val refType = this.expr.resolvedType(typeVars) as? RefType ?: return null
        val fieldName = this.structFieldRef.referenceName ?: return null
        val referredStruct = refType.referredStructDef() ?: return null
        val field = referredStruct.fieldsMap[fieldName] ?: return null

        val referredStructType = refType.referredType as? StructType ?: return null
        val structTypeVars = referredStructType.typeVars()

        val fieldType = field.typeAnnotation?.type?.resolvedType(structTypeVars) ?: return null
        return RefType(fieldType, refType.mutable)
    }
}
