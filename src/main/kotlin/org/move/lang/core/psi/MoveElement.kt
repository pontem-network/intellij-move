package org.move.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.ext.address
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.resolve.ref.MoveReference
import org.move.lang.core.types.Address

interface MoveElement : PsiElement {

    @JvmDefault
    val containingAddress: Address
        get() =
            ancestorStrict<MoveAddressDef>()?.address ?: Address.default()

    @JvmDefault
    val containingModule: MoveModuleDef?
        get() =
            ancestorStrict()
}

abstract class MoveElementImpl(node: ASTNode) : ASTWrapperPsiElement(node),
                                                MoveElement {

    override fun getReference(): MoveReference? = null
}