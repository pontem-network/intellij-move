package org.move.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.ext.address
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.types.Address

interface MoveElement : PsiElement {

    val containingAddress: Address?

    val containingModule: MoveModuleDef?
}

abstract class MoveElementImpl(node: ASTNode) : ASTWrapperPsiElement(node),
                                                MoveElement {
    override val containingAddress: Address
        get() =
            ancestorStrict<MoveAddressDef>()?.address ?: Address.default()


    override val containingModule: MoveModuleDef?
        get() =
            ancestorStrict<MoveModuleDef>()

    override fun getReference(): MoveReference? = null
}

//abstract class MoveStubbedElementImpl<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>,
//                                                                MoveElement {
//    constructor(node: ASTNode) : super(node)
//
//    constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
//
//    override fun toString(): String = "${javaClass.simpleName}($elementType)"
//}