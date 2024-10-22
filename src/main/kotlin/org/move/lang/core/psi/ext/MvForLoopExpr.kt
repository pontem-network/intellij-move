package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvForIterCondition

val MvForIterCondition.expr: MvExpr? get() = exprList.firstOrNull()
val MvForIterCondition.specExpr: MvExpr? get() = exprList.drop(1).firstOrNull()