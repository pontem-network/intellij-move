package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.move.lang.core.psi.MoveNameIdentifierOwner
import org.move.lang.core.stubs.MoveNamedElementStub

abstract class MoveStubbedNameIdentifierOwnerImpl<StubT> : MoveStubbedNamedElementImpl<StubT>,
                                                           MoveNameIdentifierOwner
        where StubT : MoveNamedElementStub, StubT : StubElement<*> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getNameIdentifier(): PsiElement? = nameElement
}