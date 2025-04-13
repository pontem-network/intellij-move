package org.move.lang.core.psi

import com.intellij.psi.PsiElement

interface AnyCodeBlock: MvElement {
    val stmtList: List<MvStmt> get() = emptyList()
    val expr: MvExpr? get() = null
    val rBrace: PsiElement? get() = null
}
