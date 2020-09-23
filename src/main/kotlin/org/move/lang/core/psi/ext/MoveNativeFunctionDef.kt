package org.move.lang.core.psi.ext

import org.move.lang.MoveElementTypes
import org.move.lang.core.psi.MoveNativeFunctionDef

val MoveNativeFunctionDef.isPublic: Boolean
    get() = isChildExists(MoveElementTypes.PUBLIC)