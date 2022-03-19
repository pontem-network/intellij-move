package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.MvElementTypes.R_BRACE
import org.move.lang.core.psi.*

val MvCodeBlock.returningExpr: MvExpr? get() = this.expr

val MvCodeBlock.rightBrace: PsiElement? get() = this.findLastChildByType(R_BRACE)

val MvCodeBlock.letStmts: List<MvLetStmt>
    get() = stmtList.filterIsInstance<MvLetStmt>()

abstract class MvCodeBlockMixin(node: ASTNode) : MvElementImpl(node), MvCodeBlock {

    override val importStmts: List<MvImportStmt> get() = importStmtList
}
