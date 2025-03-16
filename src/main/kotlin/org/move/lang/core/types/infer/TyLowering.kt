package org.move.lang.core.types.infer

import org.move.cli.settings.debugErrorOrFallback
import org.move.ide.annotator.INTEGER_TYPE_IDENTIFIERS
import org.move.ide.annotator.SPEC_INTEGER_TYPE_IDENTIFIERS
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.*
import org.move.lang.core.types.ty.*

fun MvType.loweredType(msl: Boolean): Ty = TyLowering.lowerType(this, msl)

/**
 * Returns type from the explicit instantiation of the item, like
 * ```
 * struct Option<Element> {}
 * fun main() {
 *     Option<u8>{};
 *     //^
 * }
 * ```
 * will return TyAdt(Option, Element -> u8)
 */
object TyLowering {
    fun lowerType(moveType: MvType, msl: Boolean): Ty {
        return when (moveType) {
            is MvPathType -> {
                val namedItem = moveType.path.reference?.resolveFollowingAliases()
                lowerPath(moveType.path, namedItem, msl)
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
        if (namedItem == null) {
            return if (methodOrPath is MvPath) lowerPrimitiveTy(methodOrPath, msl) else TyUnknown
        }
        val pathTy = when (namedItem) {
            is MvTypeParameter -> TyTypeParameter.named(namedItem)
            is MvSchema -> TySchema.valueOf(namedItem)
            is MvStructOrEnumItemElement -> TyAdt.valueOf(namedItem)
            is MvFunctionLike -> namedItem.functionTy(msl)
            is MvEnumVariant -> {
                // has to be MvPath of form `ENUM_NAME::ENUM_VARIANT_NAME`
                val enumPath = (methodOrPath as? MvPath)?.qualifier ?: return TyUnknown
                lowerPath(enumPath, namedItem.enumItem, msl)
            }
            else -> debugErrorOrFallback(
                "${namedItem.elementType} path cannot be inferred into type",
                TyUnknown
            )
        }
        if (namedItem is MvGenericDeclaration) {
            return (pathTy as GenericTy).applyExplicitTypeArgs(methodOrPath, namedItem, msl)
        }
        return pathTy
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
}
