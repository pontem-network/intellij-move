package org.move.lang.core.psi

import com.intellij.psi.PsiElement

interface AnyBlock: MvElement {
    val stmtList: List<MvStmt> get() = emptyList()
    val expr: MvExpr? get() = null
    val rBrace: PsiElement? get() = null
}
