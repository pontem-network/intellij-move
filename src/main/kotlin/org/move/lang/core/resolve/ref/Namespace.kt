package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.containingFunction
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.FunctionVisibility
import org.move.lang.core.psi.ext.fqModule
import org.move.lang.core.psi.ext.visibility
import org.move.lang.core.types.FQModule

sealed class Visibility {
    object Public : Visibility()
    object PublicScript : Visibility()
    class PublicFriend(val currentModule: FQModule) : Visibility()
    object Internal : Visibility()

    companion object {
        fun buildSetOfVisibilities(element: MvElement): Set<Visibility> {
            val vs = mutableSetOf<Visibility>(Public)
            val containingModule = element.containingModule
            if (containingModule != null) {
                val asFriendModule = containingModule.fqModule()
                if (asFriendModule != null) {
                    vs.add(PublicFriend(asFriendModule))
                }
            }

            val containingFun = element.containingFunction
            if (containingModule == null
                || (containingFun?.visibility == FunctionVisibility.PUBLIC_SCRIPT)
            ) {
                vs.add(PublicScript)
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
