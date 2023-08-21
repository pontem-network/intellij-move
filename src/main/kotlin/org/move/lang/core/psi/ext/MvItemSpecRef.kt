package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvItemSpecRef
import org.move.lang.core.resolve.ref.MvItemSpecRefReferenceImpl
import org.move.lang.core.resolve.ref.MvPolyVariantReference


abstract class MvItemSpecRefMixin(node: ASTNode) : MvElementImpl(node), MvItemSpecRef {

    override fun getReference(): MvPolyVariantReference? = MvItemSpecRefReferenceImpl(this)
}
