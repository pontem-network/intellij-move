package org.move.lang.core.psi

import org.move.lang.MvElementTypes
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.lang.core.psi.ext.hasChild
import org.move.lang.core.psi.ext.ty
import org.move.lang.core.psi.mixins.declaredTy
import org.move.lang.core.types.infer.foldTyTypeParameterWith
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnit
import org.move.lang.core.types.ty.TyUnknown
import org.move.stdext.chain
import org.move.stdext.wrapWithList

interface MvFunctionLike : MvTypeParametersOwner,
                           MvNameIdentifierOwner,
                           MvDocAndAttributeOwner {

    val functionParameterList: MvFunctionParameterList?

    val returnType: MvReturnType?
}

val MvFunctionLike.isNative get() = hasChild(MvElementTypes.NATIVE)

val MvFunctionLike.parameters get() = this.functionParameterList?.functionParameterList.orEmpty()

val MvFunctionLike.parameterBindings: List<MvBindingPat> get() = this.parameters.map { it.bindingPat }

val MvFunctionLike.returnTy: Ty
    get() {
        val returnTypeElement = this.returnType
        return if (returnTypeElement == null) {
            TyUnit
        } else {
            returnTypeElement.type?.ty() ?: TyUnknown
        }
    }

val MvFunctionLike.typeParamsUsedOnlyInReturnType: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        this.parameters
            .map { it.declaredTy(false) }
            .forEach {
                it.foldTyTypeParameterWith { paramTy -> usedTypeParams.add(paramTy.origin); paramTy }
            }
        return this.typeParameters.filter { it !in usedTypeParams }
    }

val MvFunctionLike.requiredTypeParams: List<MvTypeParameter>
    get() {
        val usedTypeParams = mutableSetOf<MvTypeParameter>()
        this.parameters
            .map { it.declaredTy(false) }
            .chain(this.returnType?.type?.ty().wrapWithList())
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
