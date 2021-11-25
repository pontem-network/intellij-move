package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.R_BRACE
import org.move.lang.core.psi.MoveCodeBlock
import org.move.lang.core.psi.MoveLetStatement
import org.move.lang.core.types.ty.HasType

val MoveCodeBlock.lastHasType: HasType?
    get() = this.children.filterIsInstance<HasType>().lastOrNull()

val MoveCodeBlock.rightBrace: PsiElement? get() = this.findLastChildByType(R_BRACE)

//val MoveCodeBlock.statementExprList: List<MoveExpr>
//    get() = statementList.mapNotNull { it.expr }

val MoveCodeBlock.letStatements: List<MoveLetStatement>
    get() = statementList.filterIsInstance<MoveLetStatement>()
