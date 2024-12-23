package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvParensExpr

fun MvParensExpr.unwrap(): MvExpr? {
    val expr = this.expr
    return if (expr !is MvParensExpr) expr else expr.unwrap()
}