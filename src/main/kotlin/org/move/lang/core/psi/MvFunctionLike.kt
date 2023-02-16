package org.move.lang.core.psi

import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.greenStub
import org.move.lang.core.psi.ext.paramAnnotationTy
import org.move.lang.core.psi.ext.hasChild
import org.move.lang.core.stubs.MvModuleStub
import org.move.lang.core.types.infer.*
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

fun MvFunctionLike.returnTypeTy(itemContext: ItemContext): Ty {
    val returnTypeElement = this.returnType
    return if (returnTypeElement == null) {
        TyUnit
    } else {
        returnTypeElement.type?.let { itemContext.getTypeTy(it) } ?: TyUnknown
    }
}

val MvFunctionLike.typeParamsUsedOnlyInReturnType: List<MvTypeParameter>
    get() {
        val msl = false
        val itemContext = this.module?.itemContext(msl) ?: ItemContext(msl)
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        this.parameters
            .map { it.paramAnnotationTy(itemContext) }
            .forEach {
                it.foldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.origin); paramTy }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }

val MvFunctionLike.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        val msl = false
        val itemContext = this.module?.itemContext(msl) ?: ItemContext(msl)
        this.parameters
            .map { it.paramAnnotationTy(itemContext) }
            .withAdded(this.returnTypeTy(itemContext))
            .forEach {
                it.foldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.origin); paramTy }
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
