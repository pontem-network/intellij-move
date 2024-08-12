package org.move.lang.core.psi.ext

import com.intellij.psi.util.PsiTreeUtil
import org.move.cli.containingMovePackage
import org.move.lang.MvElementTypes
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
enum class VisKind(val keyword: String) {
    PUBLIC("public"),
    FRIEND("public(friend)"),
    PACKAGE("public(package)"),
    SCRIPT("public(script)");
}

val MvVisibilityModifier.stubVisKind: VisKind
    get() = when {
        isPublicPackage -> PACKAGE
        isPublicFriend -> FRIEND
        isPublicScript -> SCRIPT
        isPublic -> PUBLIC
        else -> error("exhaustive")
    }

val MvVisibilityOwner.visibility2: Visibility2
    get() {
        val kind = this.visibilityModifier?.stubVisKind ?: return Visibility2.Private
        return when (kind) {
            PACKAGE -> Visibility2.Restricted.Package(lazy { containingMovePackage })
            FRIEND -> {
                val module = this.containingModule ?: return Visibility2.Private
                Visibility2.Restricted.Friend(lazy { module.friendModules })
            }
            SCRIPT -> Visibility2.Restricted.Script
            PUBLIC -> Visibility2.Public
        }
    }




