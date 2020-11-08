package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveModuleImport
import org.move.lang.core.psi.impl.MoveNamedElementImpl

abstract class MoveModuleImportMixin(node: ASTNode) : MoveNamedElementImpl(node),
                                                      MoveModuleImport {
    override val nameElement: PsiElement
        get() =
            importAlias?.identifier ?: fullyQualifiedModuleRef.identifier

//    override fun getReference(): MoveReference {
//        return MoveModuleReferenceImpl(moduleRef)
//    }

}