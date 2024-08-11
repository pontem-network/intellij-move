package org.move.lang.core.resolve.ref

import org.move.cli.MovePackage
import org.move.lang.core.psi.MvModule
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

enum class Namespace {
    NAME,
    FUNCTION,
    TYPE,
    SCHEMA,
    MODULE;
//    CONST;

    companion object {
        fun all(): Set<Namespace> {
            return EnumSet.of(NAME, FUNCTION, TYPE, SCHEMA, MODULE)
        }

        fun moduleItems(): Set<Namespace> = EnumSet.of(NAME, FUNCTION, TYPE, SCHEMA)

        fun none(): Set<Namespace> = setOf()
    }
}

val NONE = Namespace.none()
val NAMES = setOf(Namespace.NAME)
val NAMES_N_FUNCTIONS = setOf(Namespace.NAME, Namespace.FUNCTION)
//val NAMES_N_MODULES_N_TYPES_N_FUNCTIONS = setOf(Namespace.NAME, Namespace.FUNCTION)

val MODULES = setOf(Namespace.MODULE)
val FUNCTIONS = setOf(Namespace.FUNCTION)
val SCHEMAS = setOf(Namespace.SCHEMA)
val TYPES = setOf(Namespace.TYPE)

val TYPES_N_NAMES = setOf(Namespace.TYPE, Namespace.NAME)
val TYPES_N_MODULES = setOf(Namespace.TYPE, Namespace.MODULE)

val ALL_NAMESPACES = Namespace.all()
val ITEM_NAMESPACES =
    setOf(Namespace.NAME, Namespace.FUNCTION, Namespace.TYPE, Namespace.SCHEMA)