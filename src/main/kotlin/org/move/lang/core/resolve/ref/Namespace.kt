package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveElement
import org.move.lang.core.psi.ext.FunctionVisibility
import org.move.lang.core.psi.ext.fqModule
import org.move.lang.core.psi.ext.visibility
import org.move.lang.core.types.FQModule

sealed class Visibility {
    class Public : Visibility()
    class PublicScript : Visibility()
    class PublicFriend(val currentModule: FQModule) : Visibility()
    class Internal : Visibility()

    companion object {
        fun buildSetOfVisibilities(element: MoveElement): Set<Visibility> {
            val vs = mutableSetOf<Visibility>(Public())
            val containingModule = element.containingModule
            if (containingModule != null) {
                val asFriendModule = containingModule.fqModule()
                if (asFriendModule != null) {
                    vs.add(PublicFriend(asFriendModule))
                }
            }

            val containingFunSignature = element.containingFunction?.functionSignature
            if (containingModule == null
                || (containingFunSignature?.visibility == FunctionVisibility.PUBLIC_SCRIPT)
            ) {
                vs.add(PublicScript())
            }
            return vs
        }
    }
}

enum class Namespace {
    NAME,
    TYPE,
    SPEC,
    MODULE,
    STRUCT_FIELD,
    DOT_ACCESSED_FIELD;
}
