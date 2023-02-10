package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvExpr
import org.move.lang.core.psi.MvIfExpr

val MvIfExpr.returningExpr: MvExpr?
    get() {
        val codeBlock = this.codeBlock
        if (codeBlock != null) return codeBlock.returningExpr
        return this.inlineBlock?.expr
    }

val MvIfExpr.elseExpr: MvExpr?
    get() {
        val elseBlock = this.elseBlock ?: return null
        val elseCodeBlock = elseBlock.codeBlock
        if (elseCodeBlock != null) return elseCodeBlock.returningExpr
        return elseBlock.inlineBlock?.expr
    }
