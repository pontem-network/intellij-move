package org.move.lang.core.psi.ext

import com.intellij.psi.util.PsiTreeUtil
import org.move.cli.containingMovePackage
import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvVisibilityModifier
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.VisKind.*
import org.move.lang.core.resolve.ref.Visibility

interface MvVisibilityOwner: MvElement {
    val visibilityModifier: MvVisibilityModifier?
        get() = PsiTreeUtil.getStubChildOfType(this, MvVisibilityModifier::class.java)

    val visibility2: Visibility
        get() = visibilityModifier?.visibility ?: Visibility.Internal

    // restricted visibility considered as public
    val isPublic: Boolean get() = visibilityModifier != null
}

// todo: add VisibilityModifier to stubs, rename this one to VisStubKind
enum class VisKind {
//    PRIVATE,
    PUBLIC,
    FRIEND,
    PACKAGE,
    SCRIPT;
}

val MvVisibilityModifier.stubKind: VisKind
    get() = when {
        this.isPublicFriend -> FRIEND
        this.isPublicPackage -> PACKAGE
        this.isPublicScript -> SCRIPT
        this.isPublic -> PUBLIC
        else -> error("unreachable")
    }

val MvVisibilityModifier.visibility: Visibility
    get() = when (stubKind) {
        SCRIPT -> Visibility.PublicScript
        PACKAGE -> containingMovePackage?.let { Visibility.PublicPackage(it) } ?: Visibility.Public
        FRIEND ->
            containingModule?.let { Visibility.PublicFriend(it.asSmartPointer()) } ?: Visibility.Public
        PUBLIC -> Visibility.Public
    }





