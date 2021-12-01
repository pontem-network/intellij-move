package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveCallExpr
import org.move.lang.core.psi.MoveExpr
import org.move.lang.core.psi.MoveTypeArgument

val MoveCallExpr.typeArguments: List<MoveTypeArgument>
    get() {
        return this.path.typeArguments
    }

val MoveCallExpr.arguments: List<MoveExpr>
    get() {
        return this.callArguments?.exprList.orEmpty()
    }
