package org.move.lang.core.types.infer

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvMethodOrPath
import org.move.lang.core.types.infer.RsPsiSubstitution.Value

fun pathTypeParamsSubst(
    methodOrPath: MvMethodOrPath,
    genericItem: MvGenericDeclaration,
): RsPsiSubstitution {
    val typeParameters = genericItem.typeParameters
    val parent = methodOrPath.parent

    // Generic arguments are optional in expression context, e.g.
    // `let a = Foo::<u8>::bar::<u16>();` can be written as `let a = Foo::bar();`
    // if it is possible to infer `u8` and `u16` during type inference
    val isExprPath = parent is MvExpr || parent is MvPath && parent.parent is MvExpr
    val isPatPath = parent is MvPat || parent is MvPath && parent.parent is MvPat
    val areOptionalArgs = isExprPath || isPatPath

    val typeArguments = methodOrPath.typeArgumentList?.typeArgumentList?.map { it.type }
    val typeSubst = associateSubst(typeParameters, typeArguments, areOptionalArgs)
    return RsPsiSubstitution(typeSubst)
}

private fun <Param: Any, P: Any> associateSubst(
    parameters: List<Param>,
    arguments: List<P>?,
    areOptionalArgs: Boolean,
): Map<Param, Value<P>> {
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
