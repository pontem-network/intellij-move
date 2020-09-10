package org.move.lang.core.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.ext.findLastChildByType
import org.move.lang.core.stubs.MoveNamedStub

interface MoveNamedElement : MoveElement,
                             PsiNamedElement,
                             NavigatablePsiElement {
    val nameElement: PsiElement?
        get() = findLastChildByType(MoveElementTypes.IDENTIFIER)
}

abstract class MoveStubbedNamedElementImpl<StubT> : MoveStubbedElementImpl<StubT>,
                                                    MoveNameIdentifierOwner
        where StubT : MoveNamedStub, StubT : StubElement<*> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getName(): String? = nameElement?.text

    override fun setName(name: String): PsiElement {
        nameElement?.replace(MovePsiFactory(project).createIdentifier(name))
        return this
    }

    override fun getNameIdentifier(): PsiElement? = nameElement

    override fun getNavigationElement(): PsiElement = nameElement ?: this

    override fun getTextOffset(): Int = nameElement?.textOffset ?: super.getTextOffset()
}