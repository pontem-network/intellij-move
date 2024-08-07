package org.move.lang.core.psi

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.editor.colors.TextAttributesKey
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.lang.MvElementTypes
import org.move.lang.core.completion.CompletionContext
import org.move.lang.core.psi.ext.*
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.foldTyInferWith
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.infer.substitute
import org.move.lang.core.types.ty.Ty
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

val MvFunctionLike.functionItemPresentation: PresentationData?
    get() {
        val name = this.name ?: return null
        val signature = this.signatureText
        return PresentationData(
            "$name$signature",
            this.locationString(true),
            MoveIcons.FUNCTION,
            TextAttributesKey.createTextAttributesKey("public")
        )
    }

val MvFunctionLike.isNative get() = hasChild(MvElementTypes.NATIVE)

val MvFunctionLike.parameters get() = this.functionParameterList?.functionParameterList.orEmpty()

val MvFunctionLike.allParamsAsBindings: List<MvBindingPat> get() = this.parameters.map { it.bindingPat }

val MvFunctionLike.valueParamsAsBindings: List<MvBindingPat>
    get() {
        val msl = this.isMslOnlyItem
        val parameters = this.parameters
        return parameters
            .filter { it.type?.loweredType(msl) !is TyLambda }
            .map { it.bindingPat }
    }

val MvFunctionLike.lambdaParamsAsBindings: List<MvBindingPat>
    get() {
        val msl = this.isMslOnlyItem
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
                    this.parent as? MvModule
                }
            }
            // TODO:
            else -> null
        }

val MvFunctionLike.script: MvScript? get() = this.parent as? MvScript

val MvFunctionLike.signatureText: String
    get() {
        val paramsText = this.parameters.joinToSignature()
        val retType = this.returnType?.type?.text ?: ""
        val retTypeSuffix = if (retType == "") "" else ": $retType"
        return "$paramsText$retTypeSuffix"
    }

val MvFunction.selfParam: MvFunctionParameter? get() {
    if (!project.moveSettings.enableReceiverStyleFunctions) return null
    return this.parameters.firstOrNull()?.takeIf { it.name == "self" }
}

fun MvFunction.selfParamTy(msl: Boolean): Ty? = this.selfParam?.type?.loweredType(msl)

val MvFunction.isMethod get() = selfParam != null

val MvFunction.selfSignatureText: String
    get() {
        val paramsText = this.parameters.drop(1).joinToSignature()
        val retType = this.returnType?.type?.text ?: ""
        val retTypeSuffix = if (retType == "") "" else ": $retType"
        return "$paramsText$retTypeSuffix"
    }

fun MvFunctionLike.requiresExplicitlyProvidedTypeArguments(completionContext: CompletionContext?): Boolean
    {
        val msl = this.isMslOnlyItem
        val callTy = this.declaredType(msl).substitute(this.tyInfers) as TyFunction

        val inferenceCtx = InferenceContext(msl)

        callTy.paramTypes.forEach {
            inferenceCtx.combineTypes(it, it.foldTyInferWith { TyUnknown })
        }
        val expectedTy = completionContext?.expectedTy
        if (expectedTy != null && expectedTy !is TyUnknown) {
            inferenceCtx.combineTypes(callTy.retType, expectedTy)
        }
        val resolvedCallTy = inferenceCtx.resolveTypeVarsIfPossible(callTy) as TyFunction
        return resolvedCallTy.needsTypeAnnotation()
    }