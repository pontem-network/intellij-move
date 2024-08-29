package org.move.lang.core.types.infer

import org.move.cli.settings.debugErrorOrFallback
import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_INTEGER_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*

fun MvType.loweredType(msl: Boolean): Ty = TyLowering().lowerType(this, msl)

class TyLowering {
    fun lowerType(moveType: MvType, msl: Boolean): Ty {
        return when (moveType) {
            is MvPathType -> {
                val genericItem = moveType.path.reference?.resolveFollowingAliases()
                lowerPath(moveType.path, genericItem, msl)
            }
            is MvRefType -> {
                val mutability = Mutability.valueOf(moveType.mutable)
                val refInnerType = moveType.type ?: return TyReference(TyUnknown, mutability, msl)
                val innerTy = lowerType(refInnerType, msl)
                TyReference(innerTy, mutability, msl)
            }
            is MvTupleType -> {
                val innerTypes = moveType.typeList.map { lowerType(it, msl) }
                TyTuple(innerTypes)
            }
            is MvUnitType -> TyUnit
            is MvParensType -> lowerType(moveType.type, msl)
            is MvLambdaType -> {
                val paramTys = moveType.paramTypes.map { lowerType(it, msl) }
                val returnType = moveType.returnType
                val retTy = if (returnType == null) {
                    TyUnit
                } else {
                    lowerType(returnType, msl)
                }
                TyLambda(paramTys, retTy)
            }
            else -> debugErrorOrFallback(
                "${moveType.elementType} type is not inferred",
                TyUnknown
            )
        }
    }

    fun lowerPath(methodOrPath: MvMethodOrPath, namedItem: MvNamedElement?, msl: Boolean): Ty {
        // cannot do resolve() here due to circular caching for MethodCall, need to pass namedItem explicitly,
        // namedItem can be null if it's a primitive type
//        val namedItem = methodOrPath.reference?.resolveWithAliases()
        if (namedItem == null) {
            return if (methodOrPath is MvPath) lowerPrimitiveTy(methodOrPath, msl) else TyUnknown
        }
        return when (namedItem) {
            is MvTypeDeclarationElement -> {
                val baseTy = namedItem.declaredType(msl)
                val explicitSubst = instantiateTypeParamsSubstitution(methodOrPath, namedItem, msl)
                baseTy.substitute(explicitSubst)
            }
            is MvFunctionLike -> {
                val baseTy = namedItem.functionTy(msl)
                val explicitSubst = instantiateTypeParamsSubstitution(methodOrPath, namedItem, msl)
                baseTy.substitute(explicitSubst)
            }
//            is MvTypeParameter -> TyTypeParameter(namedItem)
//            is MvGenericDeclaration -> {
//            }
            is MvEnumVariant -> lowerPath(methodOrPath, namedItem.enumItem, msl)
            else -> debugErrorOrFallback(
                "${namedItem.elementType} path cannot be inferred into type",
                TyUnknown
            )
        }
    }

    private fun lowerPrimitiveTy(path: MvPath, msl: Boolean): Ty {
        val refName = path.referenceName ?: return TyUnknown
        if (msl && refName in SPEC_INTEGER_TYPE_IDENTIFIERS) return TyInteger.fromName("num")
        if (msl && refName == "bv") return TySpecBv

        val ty = when (refName) {
            in INTEGER_TYPE_IDENTIFIERS -> TyInteger.fromName(refName)
            "bool" -> TyBool
            "address" -> TyAddress
            "signer" -> TySigner
            "vector" -> {
                val argType = path.typeArguments.firstOrNull()?.type
                val itemTy = argType?.let { lowerType(it, msl) } ?: TyUnknown
                return TyVector(itemTy)
            }
            else -> TyUnknown
        }
        return ty
    }

    private fun <T: MvElement> instantiateTypeParamsSubstitution(
        methodOrPath: MvMethodOrPath,
        namedItem: T,
        msl: Boolean
    ): Substitution {
        if (namedItem !is MvGenericDeclaration) return emptySubstitution

        val psiSubstitution = pathPsiSubst(methodOrPath, namedItem)

        val typeSubst = hashMapOf<TyTypeParameter, Ty>()
        for ((param, value) in psiSubstitution.typeSubst.entries) {
            val paramTy = TyTypeParameter.named(param)
            val valueTy = when (value) {
                is RsPsiSubstitution.Value.Present -> lowerType(value.value, msl)
                is RsPsiSubstitution.Value.OptionalAbsent -> paramTy
                is RsPsiSubstitution.Value.RequiredAbsent -> TyUnknown
            }
            typeSubst[paramTy] = valueTy
        }
        return Substitution(typeSubst)
    }
}
