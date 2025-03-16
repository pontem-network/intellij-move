package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvMethodOrPath
import org.move.lang.core.types.infer.RsPsiSubstitution.Value
import org.move.lang.core.types.ty.GenericTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyTypeParameter
import org.move.lang.core.types.ty.TyUnknown

fun GenericTy.applyExplicitTypeArgs(
    methodOrPath: MvMethodOrPath,
    genericItem: MvGenericDeclaration,
    msl: Boolean
): Ty {
    val explicitTypeArgs = explicitTypeArgumentSubst(
        methodOrPath,
        genericItem.typeParameters,
        msl
    )
    return this.substitute(explicitTypeArgs)
}

private fun explicitTypeArgumentSubst(
    methodOrPath: MvMethodOrPath,
    typeParams: List<MvTypeParameter>,
    msl: Boolean
): Substitution {
    val explicitTypeParamsSubst = pathTypeParamsSubst(methodOrPath, typeParams)

    val typeSubst = hashMapOf<TyTypeParameter, Ty>()
    for ((param, value) in explicitTypeParamsSubst.typeSubst.entries) {
        val paramTy = TyTypeParameter.named(param)
        val valueTy = when (value) {
            is Value.Present -> TyLowering.lowerType(value.value, msl)
            is Value.OptionalAbsent -> paramTy
            is Value.RequiredAbsent -> TyUnknown
        }
        typeSubst[paramTy] = valueTy
    }
    return Substitution(typeSubst)
}

fun pathTypeParamsSubst(
    methodOrPath: MvMethodOrPath,
    typeParams: List<MvTypeParameter>,
): RsPsiSubstitution {
    val parent = methodOrPath.parent

    // Generic arguments are optional in expression context, e.g.
    // `let a = Foo::<u8>::bar::<u16>();` can be written as `let a = Foo::bar();`
    // if it is possible to infer `u8` and `u16` during type inference
    val isExprPath = parent is MvExpr || parent is MvPath && parent.parent is MvExpr
    val isPatPath = parent is MvPat || parent is MvPath && parent.parent is MvPat
    val areOptionalArgs = isExprPath || isPatPath

    val typeArguments = methodOrPath.typeArgumentList?.typeArgumentList?.map { it.type }
    val typeSubst = associateSubst(typeParams, typeArguments, areOptionalArgs)
    return RsPsiSubstitution(typeSubst)
}

private fun associateSubst(
    parameters: List<MvTypeParameter>,
    arguments: List<MvType>?,
    areOptionalArgs: Boolean,
): Map<MvTypeParameter, Value<MvType>> {
    return parameters.withIndex().associate { (i, param) ->
        val value =
            if (areOptionalArgs && arguments == null) {
                Value.OptionalAbsent
            } else if (arguments != null && i < arguments.size) {
                Value.Present(arguments[i])
            } else {
                Value.RequiredAbsent
            }
        param to value
    }
}

/** Similar to [Substitution], but maps PSI to PSI instead of [Ty] to [Ty] */
class RsPsiSubstitution(
    val typeSubst: Map<MvTypeParameter, Value<MvType>>,
) {
    sealed class Value<out P> {
        data object RequiredAbsent: Value<Nothing>()
        data object OptionalAbsent: Value<Nothing>()
        class Present(val value: MvType): Value<MvType>()
    }
}
