package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.stubs.MvNamedElementStub

abstract class MvStubbedNameIdentifierOwnerImpl<StubT> : MvStubbedNamedElementImpl<StubT>,
                                                           MvNameIdentifierOwner
        where StubT : MvNamedElementStub, StubT : StubElement<*> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getNameIdentifier(): PsiElement? = nameElement
}
