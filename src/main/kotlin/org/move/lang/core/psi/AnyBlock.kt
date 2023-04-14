package org.move.lang.core.psi

import com.intellij.psi.PsiElement

interface AnyBlock: MvElement {
    open val stmtList: List<MvStmt> get() = emptyList()
    open val expr: MvExpr? get() = null
    open val rBrace: PsiElement? get() = null
}
