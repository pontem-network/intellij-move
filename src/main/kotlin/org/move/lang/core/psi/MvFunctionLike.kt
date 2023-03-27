package org.move.lang.core.psi

import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.greenStub
import org.move.lang.core.psi.ext.hasChild
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.types.infer.outerItemContext
import org.move.lang.core.types.infer.visitTyVarWith
import org.move.lang.core.types.ty.TyLambda
import org.move.stdext.withAdded

interface MvFunctionLike : MvNameIdentifierOwner,
                           MvTypeParametersOwner,
                           MvDocAndAttributeOwner {

    val functionParameterList: MvFunctionParameterList?

    val returnType: MvReturnType?

    val codeBlock: MvCodeBlock?
}

val MvFunctionLike.isNative get() = hasChild(MvElementTypes.NATIVE)

val MvFunctionLike.parameters get() = this.functionParameterList?.functionParameterList.orEmpty()

val MvFunctionLike.allParamsAsBindings: List<MvBindingPat> get() = this.parameters.map { it.bindingPat }

val MvFunctionLike.valueParamsAsBindings: List<MvBindingPat>
    get() {
        val itemContext = this.outerItemContext(this.isMsl())
        val parameters = this.parameters
        return parameters
            .filter { it.typeTy(itemContext) !is TyLambda }
            .map { it.bindingPat }
    }

val MvFunctionLike.lambdaParamsAsBindings: List<MvBindingPat>
    get() {
        val itemContext = this.outerItemContext(this.isMsl())
        val parameters = this.parameters
        return parameters
            .filter { it.typeTy(itemContext) is TyLambda }
            .map { it.bindingPat }
    }

val MvFunctionLike.acquiresPathTypes: List<MvPathType>
    get() =
        when (this) {
            is MvFunction -> this.acquiresType?.pathTypeList.orEmpty()
            else -> emptyList()
        }

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
