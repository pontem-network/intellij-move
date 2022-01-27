package org.move.lang.core.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.findLastChildByType

interface MvNamedElement : MvElement,
                             PsiNamedElement,
                             NavigatablePsiElement {
    val nameElement: PsiElement?
        get() = findLastChildByType(MvElementTypes.IDENTIFIER)
}

//abstract class MvStubbedNamedElementImpl<StubT> : MvStubbedElementImpl<StubT>,
//                                                    MvNameIdentifierOwner
//        where StubT : MvNamedStub, StubT : StubElement<*> {
//
//    constructor(node: ASTNode) : super(node)
//
//    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
//
//    override fun getName(): String? = nameElement?.text
//
//    override fun setName(name: String): PsiElement {
//        nameElement?.replace(MvPsiFactory(project).createIdentifier(name))
//        return this
//    }
//
//    override fun getNameIdentifier(): PsiElement? = nameElement
//
//    override fun getNavigationElement(): PsiElement = nameElement ?: this
//
//    override fun getTextOffset(): Int = nameElement?.textOffset ?: super.getTextOffset()
//}
