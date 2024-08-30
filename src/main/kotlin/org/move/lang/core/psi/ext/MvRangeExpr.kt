package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvRangeExpr

val MvRangeExpr.fromExpr: MvExpr get() = exprList.first()
val MvRangeExpr.toExpr: MvExpr? get() = exprList.drop(1).firstOrNull()