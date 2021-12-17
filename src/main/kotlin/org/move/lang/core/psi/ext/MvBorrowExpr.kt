package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvBorrowExpr

val MvBorrowExpr.isMut: Boolean get() = hasChild(MvElementTypes.MUT)
