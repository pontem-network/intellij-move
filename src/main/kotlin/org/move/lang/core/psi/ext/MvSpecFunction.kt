package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvSpecFunction
import org.move.lang.core.psi.MvSpecInlineFunction
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.module
import org.move.lang.core.stubs.MvSpecFunctionStub
import org.move.lang.core.stubs.MvStubbedNamedElementImpl
import javax.swing.Icon

abstract class MvSpecFunctionMixin : MvStubbedNamedElementImpl<MvSpecFunctionStub>,
                                     MvSpecFunction {

    constructor(node: ASTNode) : super(node)

    constructor(stub: MvSpecFunctionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override val fqName: String
        get() {
            val moduleFqName = this.module?.fqName?.let { "$it::" }
            val name = this.name ?: "<unknown>"
            return moduleFqName + name
        }

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION

}

abstract class MvSpecInlineFunctionMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                          MvSpecInlineFunction {

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION

}
