package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.stubs.MvSchemaStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.ItemQualName
import org.move.lang.core.types.infer.emptySubstitution
import org.move.lang.core.types.infer.foldTyTypeParameterWith
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.GenericTy
import org.move.lang.core.types.ty.TySchema
import org.move.lang.core.types.ty.TyUnknown

val MvSchema.specBlock: MvSpecCodeBlock? get() = this.childOfType()

val MvSchema.module: MvModule?
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as? MvModule
    }

val MvSchema.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
//        val inferenceCtx = InferenceContext.default(true, this)
        this.fieldStmts
            .map { it.type?.loweredType(true) ?: TyUnknown }
//            .map { it.annotationTy(inferenceCtx) }
            .forEach {
                it.foldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.origin); paramTy }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }

val MvSchema.fieldStmts get() = this.specBlock?.schemaFields().orEmpty()

val MvSchema.fieldBindings get() = this.fieldStmts.map { it.bindingPat }

abstract class MvSchemaMixin : MvStubbedNamedElementImpl<MvSchemaStub>,
                               MvSchema {

    constructor(node: ASTNode) : super(node)

    constructor(stub: MvSchemaStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getIcon(flags: Int) = MoveIcons.SCHEMA

    override val qualName: ItemQualName?
        get() {
            val itemName = this.name ?: return null
            val moduleFQName = this.module?.qualName ?: return null
            return ItemQualName(this, moduleFQName.address, moduleFQName.itemName, itemName)
        }

    override fun declaredType(msl: Boolean): GenericTy = TySchema(this, emptySubstitution)
}
