package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvSpecFunction
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.mixins.declaredTy
import org.move.lang.core.types.infer.foldTyTypeParameterWith
import javax.swing.Icon

abstract class MvSpecFunctionMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                    MvSpecFunction {

    override fun getIcon(flags: Int): Icon = MvIcons.FUNCTION
}
