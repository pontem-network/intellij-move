package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.stubs.MvSchemaStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.foldTyTypeParameterWith

val MvSchema.specBlock: MvItemSpecBlock? get() = this.childOfType()

val MvSchema.module: MvModule
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as MvModule
    }

//val MvSchema.typeParams get() = typeParameterList?.typeParameterList.orEmpty()

val MvSchema.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        this.fieldStmts
            .map { it.declarationTypeTy(InferenceContext(true)) }
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

    override val fqName: String
        get() {
            val moduleFqName = "${this.module.fqName}::"
            val name = this.name ?: "<unknown>"
            return moduleFqName + name
        }
}
