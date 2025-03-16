package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.deepFoldTyTypeParameterWith
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.TyUnknown

val MvSchema.specBlock: MvSpecCodeBlock? get() = this.childOfType()

val MvSchema.parentModule: MvModule?
    get() {
        val parent = this.parent
        if (parent is MvModule) return parent
        if (parent is MvModuleSpecBlock) {
            return parent.moduleSpec.moduleItem
        }
        return null
    }

val MvSchema.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        this.fieldStmts
            .map { it.type?.loweredType(true) ?: TyUnknown }
            .forEach {
                it.deepFoldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.origin); paramTy }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }

val MvSchema.fieldStmts: List<MvSchemaFieldStmt> get() = this.specBlock?.schemaFields().orEmpty()

val MvSchema.fieldsAsBindings get() = this.fieldStmts.map { it.patBinding }

val MvIncludeStmt.expr: MvExpr? get() = this.childOfType()

abstract class MvSchemaMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                             MvSchema {

    override fun getIcon(flags: Int) = MoveIcons.SCHEMA
}
