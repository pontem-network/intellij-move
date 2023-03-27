package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.stubs.MvSchemaStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.ItemFQName
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.foldTyTypeParameterWith

val MvSchema.specBlock: MvItemSpecBlock? get() = this.childOfType()

val MvSchema.module: MvModule?
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as? MvModule
    }

val MvSchema.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        val inferenceCtx = InferenceContext.default(true, this)
        this.fieldStmts
            .map { it.annotationTy(inferenceCtx) }
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

    override val fqName: ItemFQName
        get() {
            val moduleFQName = this.module?.fqName ?: ItemFQName.DEFAULT_MOD_FQ_NAME
            val itemName = this.name ?: "<unknown_schema>"
            return ItemFQName(moduleFQName.address, moduleFQName.itemName, itemName)
        }
}
