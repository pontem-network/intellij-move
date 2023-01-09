package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvAttr
import org.move.lang.core.psi.MvAttrItem
import org.move.lang.core.psi.impl.MvNamedElementImpl

val MvAttrItem.attr: MvAttr get() = this.parent as MvAttr

abstract class MvAttrItemMixin(node: ASTNode) : MvNamedElementImpl(node), MvAttrItem
