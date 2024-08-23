package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes.*
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvVisibilityModifier

val MvVisibilityModifier.hasPublic get() = hasChild(PUBLIC)
val MvVisibilityModifier.hasScript get() = hasChild(SCRIPT)
val MvVisibilityModifier.hasPackage get() = hasChild(PACKAGE)
val MvVisibilityModifier.hasFriend get() = hasChild(FRIEND)

val MvVisibilityModifier.function: MvFunction? get() = parent as? MvFunction