package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes.POST
import org.move.lang.core.psi.MvLetStmt

val MvLetStmt.isPost: Boolean get() = this.hasChild(POST)
