package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MoveDotExpr
import org.move.lang.core.psi.MoveElementImpl
import org.move.lang.core.psi.MoveRefExpr
import org.move.lang.core.types.TypeVarsMap
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyReference
import org.move.lang.core.types.ty.TyStruct
import org.move.lang.core.types.ty.TyUnknown

val MoveDotExpr.refExpr: MoveRefExpr?
    get() {
        return this.expr as? MoveRefExpr
    }

abstract class MoveDotExprMixin(node: ASTNode) : MoveElementImpl(node), MoveDotExpr {
    override fun resolvedType(typeVars: TypeVarsMap): Ty {
        val objectType = this.expr.resolvedType(typeVars)
        val structType =
            when (objectType) {
                is TyReference -> objectType.referenced as? TyStruct
                is TyStruct -> objectType
                else -> null
            }
        if (structType == null) return TyUnknown

        val fieldName = this.structFieldRef.referenceName
        val field = structType.item.structDef?.fieldsMap?.get(fieldName) ?: return TyUnknown

//        val fieldTypeVars = structType.typeVars()
        return field.typeAnnotation?.type?.resolvedType(emptyMap()) ?: TyUnknown
    }
}
