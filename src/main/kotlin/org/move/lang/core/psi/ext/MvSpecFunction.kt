package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.lang.core.psi.MvSpecFunction
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl

val MvSpecFunction.typeParameters get() = this.typeParameterList?.typeParameterList.orEmpty()

val MvSpecFunction.parameters get() = this.functionParameterList?.functionParameterList.orEmpty()
val MvSpecFunction.parameterBindings get() = this.parameters.map { it.bindingPat }

abstract class MvSpecFunctionMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node), MvSpecFunction {

}
