package org.move.lang.core.psi.mixins

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.MoveStubbedNamedElementImpl
import org.move.lang.core.stubs.MoveFunctionDefStub
import javax.swing.Icon

abstract class MoveFunctionDefMixin : MoveStubbedNamedElementImpl<MoveFunctionDefStub>,
                                      MoveFunctionDef {
    constructor(node: ASTNode) : super(node)

    constructor(stub: MoveFunctionDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getIcon(flags: Int): Icon = MoveIcons.FUNCTION
}