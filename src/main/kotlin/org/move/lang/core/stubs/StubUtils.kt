package org.move.lang.core.stubs

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNameIdentifierOwner
import org.move.lang.core.psi.MvPsiFactory

abstract class MvStubbedElementImpl<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>, MvElement {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun toString(): String = "${javaClass.simpleName}($elementType)"
}

abstract class MvStubbedNamedElementImpl<StubT> : MvStubbedElementImpl<StubT>,
                                                  MvNameIdentifierOwner
        where StubT : MvNamedStub, StubT : StubElement<*> {

    constructor(node: ASTNode) : super(node)

    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun getNameIdentifier(): PsiElement? = findChildByType(MvElementTypes.IDENTIFIER)

    override fun getName(): String? {
        val stub = greenStub
        return if (stub !== null) stub.name else nameIdentifier?.text
    }

    override fun setName(name: String): PsiElement? {
        nameIdentifier?.replace(MvPsiFactory(project).identifier(name))
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}
