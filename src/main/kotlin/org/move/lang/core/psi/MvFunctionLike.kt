package org.move.lang.core.psi

import com.intellij.psi.util.descendantsOfType
import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.*
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyLambda
import org.move.lang.core.types.ty.TyUnknown

interface MvFunctionLike : MvNameIdentifierOwner,
                           MvTypeParametersOwner,
                           MvDocAndAttributeOwner {

    val functionParameterList: MvFunctionParameterList?

    val returnType: MvReturnType?

    override fun declaredType(msl: Boolean): TyFunction {
        val typeParameters = this.tyTypeParams
        val paramTypes = parameters.map { it.type?.loweredType(msl) ?: TyUnknown }
        val acquiredTypes = this.acquiresPathTypes.map { it.loweredType(msl) }
        val retType = rawReturnType(msl)
        return TyFunction(
            this,
            typeParameters,
            paramTypes,
            acquiredTypes,
            retType
        )
    }
}

val MvFunctionLike.isNative get() = hasChild(MvElementTypes.NATIVE)

val MvFunctionLike.parameters get() = this.functionParameterList?.functionParameterList.orEmpty()

val MvFunctionLike.allParamsAsBindings: List<MvBindingPat> get() = this.parameters.map { it.bindingPat }

val MvFunctionLike.valueParamsAsBindings: List<MvBindingPat>
    get() {
        val msl = this.isMsl()
        val parameters = this.parameters
        return parameters
            .filter { it.type?.loweredType(msl) !is TyLambda }
            .map { it.bindingPat }
    }

val MvFunctionLike.lambdaParamsAsBindings: List<MvBindingPat>
    get() {
        val msl = this.isMsl()
        val parameters = this.parameters
        return parameters
            .filter { it.type?.loweredType(msl) is TyLambda }
            .map { it.bindingPat }
    }

val MvFunctionLike.acquiresPathTypes: List<MvPathType>
    get() =
        when (this) {
            is MvFunction -> this.acquiresType?.pathTypeList.orEmpty()
            else -> emptyList()
        }

val MvFunctionLike.anyBlock: AnyBlock?
    get() = when (this) {
        is MvFunction -> this.codeBlock
        is MvSpecFunction -> this.specCodeBlock
        is MvSpecInlineFunction -> this.specCodeBlock
        else -> null
    }

val MvFunctionLike.module: MvModule?
    get() =
        when (this) {
            is MvFunction -> {
                val moduleStub = greenStub?.parentStub as? MvModuleStub
                if (moduleStub != null) {
                    moduleStub.psi
                } else {
                    this.parent.parent as? MvModule
                }
            }
            // TODO:
            else -> null
        }

val MvFunctionLike.script: MvScript?
    get() {
        val scriptBlock = this.parent ?: return null
        return scriptBlock.parent as? MvScript
    }
