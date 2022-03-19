package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvCallExpr
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvTypeArgument

val MvCallExpr.typeArguments: List<MvTypeArgument>
    get() {
        return this.path.typeArguments
    }

val MvCallExpr.arguments: List<MvExpr>
    get() {
        return this.callArgumentList?.exprList.orEmpty()
    }
