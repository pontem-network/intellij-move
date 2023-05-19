package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.types.ty.Ty

val MvCallExpr.typeArguments: List<MvTypeArgument> get() = this.path.typeArguments

val MvCallExpr.valueArguments: List<MvValueArgument>
    get() =
        this.valueArgumentList?.valueArgumentList.orEmpty()

val MvMacroCallExpr.valueArguments: List<MvValueArgument>
    get() =
        this.valueArgumentList?.valueArgumentList.orEmpty()

val MvCallExpr.callArgumentExprs: List<MvExpr?> get() = this.valueArguments.map { it.expr }

val MvMacroCallExpr.callArgumentExprs: List<MvExpr?> get() = this.valueArguments.map { it.expr }
