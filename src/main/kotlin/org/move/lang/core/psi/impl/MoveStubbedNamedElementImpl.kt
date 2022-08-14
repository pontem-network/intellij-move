package org.move.lang.core.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.stubs.MvNamedElementStub

abstract class MvStubbedNamedElementImpl<StubT> : MvStubbedElementImpl<StubT>,
                                                  MvNamedElement
        where StubT : MvNamedElementStub, StubT : StubElement<*> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getName(): String? {
        val stub = greenStub
        return if (stub !== null) stub.name else nameElement?.text
    }

    override fun setName(name: String): PsiElement {
        val newIdentifier = project.psiFactory.identifier(name)
        nameElement?.replace(newIdentifier)
        return this
    }

    override fun getNavigationElement(): PsiElement = nameElement ?: this

    override fun getTextOffset(): Int = nameElement?.textOffset ?: super.getTextOffset()
}
