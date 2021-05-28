package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MoveElement
import org.move.lang.core.psi.ext.asFriendModule
import org.move.lang.core.types.FriendModule

sealed class Visibility {
    class Public : Visibility()
    class PublicScript : Visibility()
    class PublicFriend(val module: FriendModule) : Visibility()

    companion object {
        fun buildSetOfVisibilities(element: MoveElement): Set<Visibility> {
            val vs = mutableSetOf<Visibility>(Public())
            val containingModule = element.containingModule
            if (containingModule != null) {
                val asFriendModule = containingModule.asFriendModule()
                if (asFriendModule != null) {
                    vs.add(PublicFriend(asFriendModule))
                }
            } else {
                vs.add(PublicScript())
            }
            return vs
        }
    }
}

enum class Namespace {
    NAME,
    TYPE,
    SCHEMA,
    MODULE,
    STRUCT_FIELD,
    DOT_ACCESSED_FIELD;
}
