package org.move.lang.core.psi.ext

import org.move.lang.MvElementTypes
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvVisibilityModifier

val MvVisibilityModifier.isPublicPackage get() = hasChild(MvElementTypes.PACKAGE)
val MvVisibilityModifier.isPublicScript get() = hasChild(MvElementTypes.SCRIPT_KW)
val MvVisibilityModifier.isPublicFriend get() = hasChild(MvElementTypes.FRIEND)
val MvVisibilityModifier.isPublic
    get() = !isPublicFriend && !isPublicScript && !isPublicPackage
            && hasChild(MvElementTypes.PUBLIC)

val MvVisibilityModifier.function: MvFunction? get() = parent as? MvFunction