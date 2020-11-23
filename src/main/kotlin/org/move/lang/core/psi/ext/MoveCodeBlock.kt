package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveCodeBlock
import org.move.lang.core.psi.MoveExpr
import org.move.lang.core.psi.MoveLetStatement

val MoveCodeBlock.statementExprList: List<MoveExpr>
    get() = statementList.mapNotNull { it.expr }

val MoveCodeBlock.letStatements: List<MoveLetStatement>
    get() = statementList.mapNotNull { it.letStatement }