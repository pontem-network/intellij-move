package org.move.lang.core.psi

import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.declarationTypeTy
import org.move.lang.core.psi.ext.hasChild
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.MvInferenceContextOwner
import org.move.lang.core.types.infer.foldTyTypeParameterWith
import org.move.lang.core.types.infer.inferTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnit
import org.move.lang.core.types.ty.TyUnknown
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

fun MvFunctionLike.returnTypeTy(inferenceCtx: InferenceContext): Ty {
    val returnTypeElement = this.returnType
    return if (returnTypeElement == null) {
        TyUnit
    } else {
        returnTypeElement.type?.let { inferTypeTy(it, inferenceCtx) } ?: TyUnknown
    }
}

val MvFunctionLike.typeParamsUsedOnlyInReturnType: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        this.parameters
            .map { it.declarationTypeTy(InferenceContext(false)) }
            .forEach {
                it.foldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.origin); paramTy }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }

val MvFunctionLike.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        val inferenceCtx = InferenceContext(false)
        this.parameters
            .map { it.declarationTypeTy(inferenceCtx) }
            .withAdded(this.returnTypeTy(inferenceCtx))
            .forEach {
                it.foldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.origin); paramTy }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }


val MvFunctionLike.module: MvModule?
    get() {
        val moduleBlock = this.parent
        return moduleBlock.parent as? MvModule
    }

val MvFunctionLike.script: MvScript?
    get() {
        val scriptBlock = this.parent
        return scriptBlock.parent as? MvScript
    }

//val MvFunctionLike.fqName: String
//    get() {
//        val moduleFqName = this.module?.fqName?.let { "$it::" }
//        val name = this.name ?: "<unknown>"
//        return moduleFqName + name
//    }

val MvFunctionLike.acquiresPathTypes: List<MvPathType>
    get() =
        when (this) {
            is MvFunction -> this.acquiresType?.pathTypeList.orEmpty()
            else -> emptyList()
        }
