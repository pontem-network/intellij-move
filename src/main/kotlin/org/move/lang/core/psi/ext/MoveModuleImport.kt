package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MoveModuleImport
import org.move.lang.core.psi.impl.MoveNamedElementImpl
import javax.swing.Icon

abstract class MoveModuleImportMixin(node: ASTNode) : MoveNamedElementImpl(node),
                                                      MoveModuleImport {
    override val nameElement: PsiElement?
        get() =
            importAlias?.identifier ?: fqModuleRef.identifier

    override fun getIcon(flags: Int): Icon = MoveIcons.MODULE

//    override fun getReference(): MoveReference {
//        return MoveModuleReferenceImpl(moduleRef)
//    }

}
