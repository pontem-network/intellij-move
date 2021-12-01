package org.move.lang.core.psi.ext

import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveBorrowExpr

val MoveBorrowExpr.isMut: Boolean
    get() {
        return childrenByType(MoveElementTypes.MUT).toList().isNotEmpty()
    }
