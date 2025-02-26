package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.MvConst
import org.move.lang.core.psi.MvEnum
import org.move.lang.core.psi.MvEnumVariant
import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.psi.MvGlobalVariableStmt
import org.move.lang.core.psi.MvLabelDecl
import org.move.lang.core.psi.MvModule
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.MvSchema
import org.move.lang.core.psi.MvStruct
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.ext.MvFieldDecl
import org.move.lang.core.resolve.ScopeEntry
import org.move.stdext.intersects
import java.util.*

sealed class Visibility {
    data object Public: Visibility()
    data object Private: Visibility()
    sealed class Restricted: Visibility() {
        class Friend(): Restricted()
        class Package(): Restricted()
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
            return EnumSet.of(NAME, TYPE, ENUM, SCHEMA, MODULE)
        }

        fun none(): Set<Namespace> = setOf()
    }
}

val NONE = Namespace.none()
val NAMES = setOf(Namespace.NAME)

val MODULES = setOf(Namespace.MODULE)
val SCHEMAS = setOf(Namespace.SCHEMA)
val TYPES = setOf(Namespace.TYPE)
val ENUMS = setOf(Namespace.ENUM)

val TYPES_N_ENUMS = setOf(Namespace.TYPE, Namespace.ENUM)
val TYPES_N_NAMES = setOf(Namespace.TYPE, Namespace.NAME)
val TYPES_N_ENUMS_N_NAMES = setOf(Namespace.TYPE, Namespace.NAME, Namespace.ENUM)
val ENUMS_N_MODULES = setOf(Namespace.ENUM, Namespace.MODULE)
val TYPES_N_ENUMS_N_MODULES = setOf(Namespace.TYPE, Namespace.ENUM, Namespace.MODULE)

val ALL_NAMESPACES = Namespace.all()
val ITEM_NAMESPACES =
    setOf(Namespace.NAME, Namespace.TYPE, Namespace.ENUM, Namespace.SCHEMA)

val MvNamedElement.itemNs
    get() = when (this) {
        is MvModule -> MODULES
        is MvFunctionLike -> NAMES
        is MvTypeParameter -> TYPES
        is MvStruct -> TYPES
        is MvEnum -> ENUMS
        is MvEnumVariant -> TYPES_N_NAMES
        is MvPatBinding -> NAMES
        is MvFieldDecl -> NAMES
        is MvConst -> NAMES
        is MvSchema -> SCHEMAS
        is MvGlobalVariableStmt -> NAMES
        is MvLabelDecl -> NONE
        else -> error("when should be exhaustive, $this is not covered")
    }

fun List<ScopeEntry>.filterByNs(ns: Set<Namespace>): List<ScopeEntry> {
    return this.filter {
        it.namespaces.intersects(ns)
    }
}