package org.move.lang.core.psi.ext

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvVisibilityModifier
import org.move.lang.core.psi.ext.VisKind.*
import org.move.lang.core.resolve.ref.Visibility2

interface MvVisibilityOwner: MvElement {
    val visibilityModifier: MvVisibilityModifier? get() = childOfType<MvVisibilityModifier>()
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
        hasFriend -> FRIEND
        hasPackage -> PACKAGE
        hasPublic -> PUBLIC
        // deprecated, should be at the end
        hasScript -> SCRIPT
        else -> error("exhaustive")
    }

val MvVisibilityOwner.visibility2: Visibility2
    get() {
        val kind = this.visibilityModifier?.stubVisKind ?: return Visibility2.Private
        return when (kind) {
            PACKAGE -> Visibility2.Restricted.Package()
            FRIEND -> {
                Visibility2.Restricted.Friend(/*lazy { module.friendModules }*/)
            }
            // public(script) == public entry
            SCRIPT -> Visibility2.Public
            PUBLIC -> Visibility2.Public
        }
    }




