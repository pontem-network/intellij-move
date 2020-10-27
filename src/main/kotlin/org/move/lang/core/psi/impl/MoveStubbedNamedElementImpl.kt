package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MovePsiFactory
import org.move.lang.core.stubs.MoveNamedStub

abstract class MoveStubbedNamedElementImpl<StubT> : MoveStubbedElementImpl<StubT>,
                                                    MoveNamedElement
        where StubT : MoveNamedStub, StubT : StubElement<*> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getName(): String? {
        val stub = greenStub
        return if (stub !== null) stub.name else nameElement?.text
    }

    override fun setName(name: String): PsiElement {
        nameElement?.replace(MovePsiFactory(project).createIdentifier(name))
        return this
    }

    override fun getNavigationElement(): PsiElement = nameElement ?: this

    override fun getTextOffset(): Int = nameElement?.textOffset ?: super.getTextOffset()
}