package org.move.lang.core.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.ext.ancestorOrSelf
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.toAddress
import org.move.lang.core.types.Address

interface MvElement : PsiElement {
    @JvmDefault
    val containingAddress: Address get() {
        return ancestorStrict<MvAddressDef>()
            ?.addressRef
            ?.toAddress() ?: Address.default()
    }

    @JvmDefault
    val containingModule: MvModuleDef?
        get() =
            ancestorStrict()

    @JvmDefault
    val containingScript: MvScriptDef?
        get() =
            ancestorStrict()

    @JvmDefault
    val containingFunction: MvFunction?
        get() =
            ancestorStrict()
}

val MvElement.containingImportsOwner get() = ancestorOrSelf<MvImportStatementsOwner>()

abstract class MvElementImpl(node: ASTNode) : ASTWrapperPsiElement(node),
                                                MvElement
