package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveCodeBlock
import org.move.lang.core.psi.MoveExpr

val MoveCodeBlock.statementExprList: List<MoveExpr>
    get() = statementList.map { it.expr }