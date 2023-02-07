package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvSchema
import org.move.lang.core.psi.ext.module
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.stubs.MvSchemaStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl

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
