package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MoveDotExpr
import org.move.lang.core.psi.MoveRefExpr

val MoveDotExpr.refExpr: MoveRefExpr?
    get() {
        return this.expr as? MoveRefExpr
    }
