package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvMethodOrPath
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyTypeParameter
import org.move.lang.core.types.ty.TyUnknown

fun MvMethodOrPath.typeArgsSubst(genericItem: MvGenericDeclaration, msl: Boolean): Substitution {
    val explicitTypeArgs = explicitTypeArgumentSubst(
        this,
        genericItem.typeParameters,
        msl
    )
    return explicitTypeArgs
}

private fun explicitTypeArgumentSubst(
    methodOrPath: MvMethodOrPath,
    typeParams: List<MvTypeParameter>,
    msl: Boolean
): Substitution {
    val explicitTypeParamsSubst = pathTypeParamsSubst(methodOrPath, typeParams)

    val typeSubst = hashMapOf<TyTypeParameter, Ty>()
    for ((param, value) in explicitTypeParamsSubst.entries) {
        val paramTy = TyTypeParameter.named(param)
        val valueTy = when (value) {
            is TypeArg.Present -> TyLowering.lowerType(value.value, msl)
            is TypeArg.OptionalAbsent -> paramTy
            is TypeArg.RequiredAbsent -> TyUnknown
        }
        typeSubst[paramTy] = valueTy
    }
    return Substitution(typeSubst)
}

fun pathTypeParamsSubst(
    methodOrPath: MvMethodOrPath,
    typeParams: List<MvTypeParameter>,
): PsiSubst {
    val parent = methodOrPath.parent

    // Generic arguments are optional in expression context, e.g.
    // `let a = Foo::<u8>::bar::<u16>();` can be written as `let a = Foo::bar();`
    // if it is possible to infer `u8` and `u16` during type inference
    val isExprPath = parent is MvExpr || parent is MvPath && parent.parent is MvExpr
    val isPatPath = parent is MvPat || parent is MvPath && parent.parent is MvPat
    val areOptionalArgs = isExprPath || isPatPath

    val typeArgumentList = methodOrPath.typeArgumentList
    if (typeArgumentList == null) {
        return typeParams.associate { typeParam ->
            val typeArg = if (areOptionalArgs) TypeArg.OptionalAbsent else TypeArg.RequiredAbsent
            typeParam to typeArg
        }
    }

    val typeArgs = typeArgumentList.typeArgumentList.map { it.type }

    val typeSubst = typeParams.withIndex().associate { (i, typeParam) ->
        val typeArg =
            if (i < typeArgs.size) TypeArg.Present(typeArgs[i]) else TypeArg.RequiredAbsent
        typeParam to typeArg
    }
    return typeSubst
}

//private fun associateSubst(
//    typeParams: List<MvTypeParameter>,
//    typeArguments: List<MvType>?,
//    areOptionalArgs: Boolean,
//): Map<MvTypeParameter, TypeArg<MvType>> {
//    return typeParams.withIndex().associate { (i, param) ->
//        val value =
//            if (areOptionalArgs && typeArguments == null) {
//                TypeArg.OptionalAbsent
//            } else if (typeArguments != null && i < typeArguments.size) {
//                TypeArg.Present(typeArguments[i])
//            } else {
//                TypeArg.RequiredAbsent
//            }
//        param to value
//    }
//}

/** Similar to [Substitution], but maps PSI to PSI instead of [Ty] to [Ty] */
typealias PsiSubst = Map<MvTypeParameter, TypeArg>

sealed class TypeArg {
    data object RequiredAbsent: TypeArg()
    data object OptionalAbsent: TypeArg()
    class Present(val value: MvType): TypeArg()
}

//class RsPsiSubstitution(
//    val typeSubst: Map<MvTypeParameter, Value<MvType>>,
//) {
//    sealed class Value<out P> {
//        data object RequiredAbsent: Value<Nothing>()
//        data object OptionalAbsent: Value<Nothing>()
//        class Present(val value: MvType): Value<MvType>()
//    }
//}
