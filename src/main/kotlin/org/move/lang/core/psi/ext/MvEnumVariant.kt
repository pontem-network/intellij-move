package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MoveIcons
import org.move.lang.core.psi.MvEnum
import org.move.lang.core.psi.MvEnumBody
import org.move.lang.core.psi.MvEnumVariant
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.impl.MvNamedElementImpl
import javax.swing.Icon

val MvEnumVariant.enumBody: MvEnumBody get() = this.parent as MvEnumBody
val MvEnumVariant.enumItem: MvEnum get() = this.enumBody.parent as MvEnum

abstract class MvEnumVariantMixin(node: ASTNode): MvNameIdentifierOwnerImpl(node),
                                                  MvEnumVariant {

    override fun getIcon(flags: Int): Icon = MoveIcons.ENUM_VARIANT
}