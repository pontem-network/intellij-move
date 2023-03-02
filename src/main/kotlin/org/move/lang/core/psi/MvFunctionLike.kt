package org.move.lang.core.psi

import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.greenStub
import org.move.lang.core.psi.ext.hasChild
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.types.infer.MvInferenceContextOwner
import org.move.lang.core.types.infer.outerItemContext
import org.move.lang.core.types.infer.visitTyVarWith
import org.move.stdext.withAdded

interface MvFunctionLike : MvTypeParametersOwner,
                           MvNameIdentifierOwner,
                           MvDocAndAttributeOwner,
                           MvInferenceContextOwner {

    val functionParameterList: MvFunctionParameterList?

    val returnType: MvReturnType?

    val codeBlock: MvCodeBlock?

    override fun parameterBindings(): List<MvBindingPat> =
        this.functionParameterList?.functionParameterList.orEmpty().map { it.bindingPat }
}

val MvFunctionLike.isNative get() = hasChild(MvElementTypes.NATIVE)

val MvFunctionLike.parameters get() = this.functionParameterList?.functionParameterList.orEmpty()

val MvFunctionLike.typeParamsUsedOnlyInReturnType: List<MvTypeParameter>
    get() {
        val msl = false
        val itemContext = this.outerItemContext(msl)
        val funcTy = itemContext.getFunctionItemTy(this)

        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        funcTy.paramTypes
            .forEach {
                it.visitTyVarWith { tyVar ->
                    tyVar.origin?.origin?.let { o -> usedTypeParams.add(o) }; false
                }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }

val MvFunctionLike.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        val msl = false
        val itemContext = this.outerItemContext(msl)
        val funcTy = itemContext.getFunctionItemTy(this)
        funcTy.paramTypes
            .withAdded(funcTy.retType)
            .forEach {
                it.visitTyVarWith { tyVar ->
                    tyVar.origin?.origin?.let { o -> usedTypeParams.add(o) }; false
                }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }


val MvFunctionLike.module: MvModule?
    get() {
        if (this is MvFunction) {
            val moduleStub = greenStub?.parentStub as? MvModuleStub
            if (moduleStub != null) {
                return moduleStub.psi
            }
        }
        val moduleBlock = this.parent ?: return null
        return moduleBlock.parent as? MvModule
    }

val MvFunctionLike.script: MvScript?
    get() {
        val scriptBlock = this.parent ?: return null
        return scriptBlock.parent as? MvScript
    }

val MvFunctionLike.acquiresPathTypes: List<MvPathType>
    get() =
        when (this) {
            is MvFunction -> this.acquiresType?.pathTypeList.orEmpty()
            else -> emptyList()
        }
