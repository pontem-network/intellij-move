package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes.*
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvVisibilityModifier

val MvVisibilityModifier.isPublic get() = hasChild(PUBLIC)
val MvVisibilityModifier.isPublicScript get() = hasChild(SCRIPT)
val MvVisibilityModifier.isPublicPackage get() = hasChild(PACKAGE)
val MvVisibilityModifier.isPublicFriend get() = hasChild(FRIEND)

val MvVisibilityModifier.function: MvFunction? get() = parent as? MvFunction