package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.R_BRACE
import org.move.lang.core.psi.MoveCodeBlock
import org.move.lang.core.psi.MoveExpr
import org.move.lang.core.psi.MoveLetStatement

val MoveCodeBlock.returningExpr: MoveExpr? get() = this.expr

val MoveCodeBlock.rightBrace: PsiElement? get() = this.findLastChildByType(R_BRACE)

val MoveCodeBlock.letStatements: List<MoveLetStatement>
    get() = statementList.filterIsInstance<MoveLetStatement>()
