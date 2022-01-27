package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvModuleImport
import org.move.lang.core.psi.impl.MvNamedElementImpl
import javax.swing.Icon

abstract class MvModuleImportMixin(node: ASTNode) : MvNamedElementImpl(node),
                                                    MvModuleImport {
    override val nameElement: PsiElement?
        get() =
            importAlias?.identifier ?: fqModuleRef?.identifier

    override fun getIcon(flags: Int): Icon = MvIcons.MODULE

//    override fun getReference(): MvReference {
//        return MvModuleReferenceImpl(moduleRef)
//    }

}
