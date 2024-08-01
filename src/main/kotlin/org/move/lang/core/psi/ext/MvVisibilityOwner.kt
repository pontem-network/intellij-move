package org.move.lang.core.psi.ext

import com.intellij.psi.util.PsiTreeUtil
import org.move.cli.containingMovePackage
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvVisibilityModifier
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.VisKind.*
import org.move.lang.core.resolve.ref.Visibility2

interface MvVisibilityOwner: MvElement {
    val visibilityModifier: MvVisibilityModifier?
        get() = PsiTreeUtil.getStubChildOfType(this, MvVisibilityModifier::class.java)

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

val MvVisibilityOwner.visibility2: Visibility2
    get() {
        val kind = this.visibilityModifier?.stubKind ?: return Visibility2.Private

        return when (kind) {
            PACKAGE -> containingMovePackage?.let { Visibility2.Restricted.Package(it) } ?: Visibility2.Public
            FRIEND -> {
                val module = this.containingModule ?: return Visibility2.Private
                // todo: make lazy
                Visibility2.Restricted.Friend(lazy { module.friendModules })
            }
            SCRIPT -> Visibility2.Restricted.Script
            PUBLIC -> Visibility2.Public
        }
    }




