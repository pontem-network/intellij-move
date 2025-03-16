package org.move.lang.core.psi

import org.move.cli.settings.moveSettings
import org.move.lang.MvElementTypes
import org.move.lang.core.completion.MvCompletionContext
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.deepFoldTyInferWith
import org.move.lang.core.types.infer.loweredType
import org.move.lang.core.types.infer.substitute
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyFunction
import org.move.lang.core.types.ty.TyUnknown
import org.move.lang.core.types.ty.functionTy

interface MvFunctionLike: MvNameIdentifierOwner,
                          MvGenericDeclaration,
                          MvDocAndAttributeOwner {

    val functionParameterList: MvFunctionParameterList?

    val returnType: MvReturnType?
}

val MvFunctionLike.isNative get() = hasChild(MvElementTypes.NATIVE)

val MvFunctionLike.parameters get() = this.functionParameterList?.functionParameterList.orEmpty()

val MvFunctionLike.parametersAsBindings: List<MvPatBinding> get() = this.parameters.map { it.patBinding }

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
            is MvFunction -> this.parent as? MvModule
            is MvSpecFunction -> this.parentModule
            is MvSpecInlineFunction -> this.parentModule
            // TODO:
            else -> null
        }

val MvFunctionLike.script: MvScript? get() = this.parent as? MvScript

val MvFunction.selfParam: MvFunctionParameter?
    get() {
        if (!project.moveSettings.enableReceiverStyleFunctions) return null
        return this.parameters.firstOrNull()?.takeIf { it.name == "self" }
    }
fun MvFunctionParameter.loweredTy(msl: Boolean): Ty? = this.type?.loweredType(msl)

fun MvFunctionLike.requiresExplicitlyProvidedTypeArguments(completionContext: MvCompletionContext?): Boolean {
    val msl = this.isMslOnlyItem
    val callTy = this.functionTy(msl).substitute(this.tyVarsSubst) as TyFunction

    val inferenceCtx = InferenceContext(msl)
    callTy.paramTypes.forEach {
        inferenceCtx.combineTypes(it, it.deepFoldTyInferWith { TyUnknown })
    }

    val expectedTy = completionContext?.expectedTy
    if (expectedTy != null && expectedTy !is TyUnknown) {
        inferenceCtx.combineTypes(callTy.returnType, expectedTy)
    }
    val resolvedCallTy = inferenceCtx.resolveTypeVarsIfPossible(callTy) as TyFunction

    return resolvedCallTy.needsTypeAnnotation()
}