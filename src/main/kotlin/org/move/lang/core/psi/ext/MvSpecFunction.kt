package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import org.move.ide.MvIcons
import org.move.lang.core.psi.MvSpecFunction
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.mixins.declaredTy
import org.move.lang.core.types.infer.foldTyTypeParameterWith
import javax.swing.Icon

val MvSpecFunction.typeParameters get() = this.typeParameterList?.typeParameterList.orEmpty()

val MvSpecFunction.parameters get() = this.functionParameterList?.functionParameterList.orEmpty()

val MvSpecFunction.parameterBindings get() = this.parameters.map { it.bindingPat }

val MvSpecFunction.typeParamsUsedOnlyInReturnType: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        this.parameters
            .map { it.declaredTy }
            .forEach {
                it.foldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.parameter); paramTy }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }


abstract class MvSpecFunctionMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                    MvSpecFunction {

    override fun getIcon(flags: Int): Icon = MvIcons.FUNCTION
}
