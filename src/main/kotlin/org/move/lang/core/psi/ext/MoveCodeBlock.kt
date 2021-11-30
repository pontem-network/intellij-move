package org.move.lang.core.psi.ext

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveCodeBlock
import org.move.lang.core.psi.MoveElement
import org.move.lang.core.psi.MoveExpr
import org.move.lang.core.psi.MoveLetStatement
import org.move.lang.core.types.HasType

val MoveCodeBlock.lastHasType: HasType?
    get() = this.children.filterIsInstance<HasType>().lastOrNull()

//val MoveCodeBlock.statementExprList: List<MoveExpr>
//    get() = statementList.mapNotNull { it.expr }

val MoveCodeBlock.letStatements: List<MoveLetStatement>
    get() = statementList.filterIsInstance<MoveLetStatement>()
