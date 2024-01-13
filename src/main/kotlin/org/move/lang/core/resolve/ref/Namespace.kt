package org.move.lang.core.resolve.ref

import com.intellij.psi.SmartPsiElementPointer
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingFunction
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.FunctionVisibility
import org.move.lang.core.psi.ext.smartPointer
import org.move.lang.core.psi.ext.visibility

sealed class Visibility {
    object Public : Visibility()
    object PublicScript : Visibility()
    class PublicFriend(val currentModule: SmartPsiElementPointer<MvModule>) : Visibility()
    object Internal : Visibility()

    companion object {
        fun local(): Set<Visibility> = setOf(Public, Internal)
        fun none(): Set<Visibility> = setOf()

        fun buildSetOfVisibilities(element: MvElement): Set<Visibility> {
            val vs = mutableSetOf<Visibility>(Public)
            val containingModule = element.containingModule
            if (containingModule != null) {
                vs.add(PublicFriend(containingModule.smartPointer()))
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
    FUNCTION,
    TYPE,
    SCHEMA,
    MODULE,
    CONST;

    companion object {
        fun all(): Set<Namespace> {
            return setOf(NAME, FUNCTION, TYPE, SCHEMA, MODULE, CONST)
        }

        fun none(): Set<Namespace> = setOf()
    }
}
