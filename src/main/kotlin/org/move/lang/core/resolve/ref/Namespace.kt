package org.move.lang.core.resolve.ref

import com.intellij.psi.SmartPsiElementPointer
import org.move.cli.MovePackage
import org.move.cli.containingMovePackage
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.containingFunction
import org.move.lang.core.psi.containingModule
import org.move.lang.core.psi.ext.FQModule
import org.move.lang.core.psi.ext.FunctionVisibility
import org.move.lang.core.psi.ext.asSmartPointer
import org.move.lang.core.psi.ext.visibility
import java.util.*

sealed class Visibility2 {
    data object Public: Visibility2()
    data object Private: Visibility2()
    sealed class Restricted: Visibility2() {
        class Friend(val friendModules: Lazy<Set<MvModule>>): Restricted()
        class Package(val originPackage: MovePackage): Restricted()
        data object Script: Restricted()
    }

}

sealed class Visibility {
    object Public: Visibility()
    object PublicScript: Visibility()
    class PublicFriend(val currentModule: SmartPsiElementPointer<MvModule>): Visibility()
    data class PublicPackage(val originPackage: MovePackage): Visibility()
    object Internal: Visibility()

    companion object {
        fun local(): Set<Visibility> = setOf(Public, Internal)
        fun none(): Set<Visibility> = setOf()

        fun visibilityScopesForElement(element: MvElement): Set<Visibility> {
            val vs = mutableSetOf<Visibility>(Public)
            val containingModule = element.containingModule
            if (containingModule != null) {
                vs.add(PublicFriend(containingModule.asSmartPointer()))
            }
            val containingFun = element.containingFunction
            if (containingModule == null
                || (containingFun?.visibility == FunctionVisibility.PUBLIC_SCRIPT)
            ) {
                vs.add(PublicScript)
            }
            val containingMovePackage = element.containingMovePackage
            if (containingMovePackage != null) {
                vs.add(PublicPackage(containingMovePackage))
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
            return EnumSet.of(NAME, FUNCTION, TYPE, SCHEMA, MODULE, CONST)
        }
        fun items(): Set<Namespace> = EnumSet.of(NAME, FUNCTION, TYPE, SCHEMA, CONST)

        fun none(): Set<Namespace> = setOf()
    }
}
