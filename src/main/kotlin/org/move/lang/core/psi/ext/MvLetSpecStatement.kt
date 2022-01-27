package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes.POST
import org.move.lang.core.psi.MvLetSpecStatement

val MvLetSpecStatement.isPost: Boolean get() = this.hasChild(POST)
