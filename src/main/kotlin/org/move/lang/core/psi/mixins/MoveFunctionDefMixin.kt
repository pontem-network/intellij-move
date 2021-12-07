package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvFunctionDef
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.InferenceContext
import javax.swing.Icon

abstract class MvFunctionDefMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                     MvFunctionDef {
//    constructor(node: ASTNode) : super(node)

//    constructor(stub: MvFunctionDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MvIcons.FUNCTION
}
