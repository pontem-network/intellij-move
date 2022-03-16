package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvSpecFunction
import org.move.lang.core.psi.MvSpecInlineFunction
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import javax.swing.Icon

abstract class MvSpecFunctionMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                    MvSpecFunction {

    override fun getIcon(flags: Int): Icon = MvIcons.FUNCTION
}

abstract class MvSpecInlineFunctionMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                          MvSpecInlineFunction {
    override fun getIcon(flags: Int): Icon = MvIcons.FUNCTION

}
