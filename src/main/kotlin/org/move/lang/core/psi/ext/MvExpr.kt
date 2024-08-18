package org.move.lang.core.psi.ext

import org.move.lang.core.psi.*
import org.move.lang.core.resolve2.ref.InferenceCachedPathElement

val MvExpr.isAtomExpr: Boolean get() =
    this is MvAnnotatedExpr
            || this is MvTupleLitExpr
            || this is MvParensExpr
            || this is MvVectorLitExpr
            || this is MvDotExpr
            || this is MvIndexExpr
            || this is MvCallExpr
            || this is MvPathExpr
            || this is MvLambdaExpr
            || this is MvLitExpr
            || this is MvCodeBlockExpr

val MvIndexExpr.receiverExpr: MvExpr get() = exprList.first()

val MvIndexExpr.argExpr: MvExpr get() = exprList.drop(1).first()

val MvExpr.declaration: MvElement?
    get() = when (this) {
        is InferenceCachedPathElement -> path.reference?.resolve()
        is MvDotExpr -> expr.declaration
        is MvIndexExpr -> receiverExpr.declaration
        else -> null
    }
