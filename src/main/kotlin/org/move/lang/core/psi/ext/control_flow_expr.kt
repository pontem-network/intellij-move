package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveExpr
import org.move.lang.core.psi.MoveIfExpr

val MoveIfExpr.returningExpr: MoveExpr? get() {
    val codeBlock = this.codeBlock
    if (codeBlock != null) return codeBlock.returningExpr
    return this.inlineBlock?.expr
}

val MoveIfExpr.elseExpr: MoveExpr? get() {
    val elseBlock = this.elseBlock ?: return null
    val elseCodeBlock = elseBlock.codeBlock
    if (elseCodeBlock != null) return elseCodeBlock.returningExpr
    return elseBlock.inlineBlock?.expr
}
