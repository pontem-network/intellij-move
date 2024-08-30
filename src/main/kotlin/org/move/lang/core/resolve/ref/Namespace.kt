package org.move.lang.core.resolve.ref

import org.move.cli.MovePackage
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.ext.MvVisibilityOwner
import java.util.*

sealed class Visibility2 {
    data object Public: Visibility2()
    data object Private: Visibility2()
    sealed class Restricted: Visibility2() {
        class Friend(/*val friendModules: Lazy<Set<MvModule>>*/): Restricted()
        class Package(/*val contextElement: MvVisibilityOwner*/): Restricted()
    }

}

enum class Namespace {
    NAME,
    FUNCTION,
    TYPE,
    ENUM,
    SCHEMA,
    MODULE;

    companion object {
        fun all(): Set<Namespace> {
            return EnumSet.of(NAME, FUNCTION, TYPE, ENUM, SCHEMA, MODULE)
        }

        fun none(): Set<Namespace> = setOf()
    }
}

val NONE = Namespace.none()
val NAMES = setOf(Namespace.NAME)
//val NAMES_N_FUNCTIONS = setOf(Namespace.NAME, Namespace.FUNCTION)

val MODULES = setOf(Namespace.MODULE)
val FUNCTIONS = setOf(Namespace.FUNCTION)
val SCHEMAS = setOf(Namespace.SCHEMA)
val TYPES = setOf(Namespace.TYPE)
val ENUMS = setOf(Namespace.ENUM)
val TYPES_N_ENUMS = setOf(Namespace.TYPE, Namespace.ENUM)

val TYPES_N_NAMES = setOf(Namespace.TYPE, Namespace.NAME)
val ENUMS_N_MODULES = setOf(Namespace.ENUM, Namespace.MODULE)
val TYPES_N_ENUMS_N_MODULES = setOf(Namespace.TYPE, Namespace.ENUM, Namespace.MODULE)
//val TYPES_N_ENUMS_N_MODULES_N_NAMES = setOf(Namespace.TYPE, Namespace.ENUM, Namespace.MODULE, Namespace.NAME)

val ALL_NAMESPACES = Namespace.all()
val ITEM_NAMESPACES =
    setOf(Namespace.NAME, Namespace.FUNCTION, Namespace.TYPE, Namespace.ENUM, Namespace.SCHEMA)