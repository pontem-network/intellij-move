package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.R_BRACE
import org.move.lang.core.psi.MvCodeBlock
import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvLetStmt

val MvCodeBlock.returningExpr: MvExpr? get() = this.expr

val MvCodeBlock.rightBrace: PsiElement? get() = this.findLastChildByType(R_BRACE)

val MvCodeBlock.letStmts: List<MvLetStmt>
    get() = stmtList.filterIsInstance<MvLetStmt>()
