package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvTupleFieldDecl
import javax.swing.Icon

val MvTupleFieldDecl.position: Int?
    get() = owner?.positionalFields?.withIndex()?.firstOrNull { it.value === this }?.index

abstract class MvTupleFieldDeclMixin(node: ASTNode): MvElementImpl(node), MvTupleFieldDecl {

    override fun getIcon(flags: Int): Icon? = MoveIcons.FIELD

    override fun getName(): String? = position?.toString()
}
