package org.move.lang.core.resolve.ref

import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.MvFieldDecl
import org.move.lang.core.resolve.scopeEntry.ScopeEntry
import org.move.stdext.emptyEnumSet
import java.util.*

sealed class Visibility {
    data object Public: Visibility()
    data object Private: Visibility()
    sealed class Restricted: Visibility() {
        class Friend(): Restricted()
        class Package(): Restricted()
    }
}

enum class Ns {
    NAME,
    FUNCTION,
    TYPE,
    ENUM,
    ENUM_VARIANT,
    SCHEMA,
    MODULE;

    companion object {
        fun all(): NsSet = EnumSet.allOf(Ns::class.java)
    }
}
typealias NsSet = EnumSet<Ns>

fun nsset(): NsSet = emptyEnumSet()
fun nsset(ns1: Ns): NsSet = NsSet.of(ns1)
fun nsset(ns1: Ns, ns2: Ns): NsSet = NsSet.of(ns1, ns2)
fun nsset(ns1: Ns, ns2: Ns, ns3: Ns): NsSet = NsSet.of(ns1, ns2, ns3)
fun nsset(ns1: Ns, ns2: Ns, ns3: Ns, ns4: Ns): NsSet = NsSet.of(ns1, ns2, ns3, ns4)

fun NsSet.intersects(other: NsSet): Boolean = other.any { this.contains(it) }
fun NsSet.add(other: NsSet): NsSet {
    return EnumSet.copyOf(this.plus(other))
}
fun NsSet.sub(other: NsSet): NsSet {
    val res = this.minus(other)
    return if (res.isEmpty()) {
        NONE
    } else {
        NsSet.copyOf(res)
    }
}

val NONE = nsset()
val NAMES = nsset(Ns.NAME)
val ENUM_VARIANTS = nsset(Ns.ENUM_VARIANT)

val MODULES = nsset(Ns.MODULE)
val SCHEMAS = nsset(Ns.SCHEMA)
val TYPES = nsset(Ns.TYPE)
val ENUMS = nsset(Ns.ENUM)
val FUNCTIONS = nsset(Ns.FUNCTION)

val ENUMS_N_MODULES = nsset(Ns.ENUM, Ns.MODULE)
val TYPES_N_ENUMS_N_MODULES = nsset(Ns.TYPE, Ns.ENUM, Ns.MODULE)

val TYPES_N_ENUMS = nsset(Ns.TYPE, Ns.ENUM)
val TYPES_N_ENUMS_N_ENUM_VARIANTS = nsset(Ns.TYPE, Ns.ENUM, Ns.ENUM_VARIANT)
val TYPES_N_ENUMS_N_ENUM_VARIANTS_N_MODULES = nsset(Ns.TYPE, Ns.ENUM, Ns.ENUM_VARIANT, Ns.MODULE)
val NAMES_N_ENUM_VARIANTS = nsset(Ns.NAME, Ns.ENUM_VARIANT)
val NAMES_N_FUNCTIONS = nsset(Ns.NAME, Ns.FUNCTION)
val NAMES_N_FUNCTIONS_N_ENUM_VARIANTS = nsset(Ns.NAME, Ns.FUNCTION, Ns.ENUM_VARIANT)
val TYPES_N_NAMES = nsset(Ns.TYPE, Ns.NAME)
val TYPES_N_ENUMS_N_NAMES = nsset(Ns.TYPE, Ns.ENUM, Ns.NAME)

val ALL_NS = Ns.all()
val IMPORTABLE_NS: NsSet = EnumSet.of(Ns.NAME, Ns.FUNCTION, Ns.TYPE, Ns.SCHEMA, Ns.ENUM)

val MvNamedElement.itemNs
    get() = when (this) {
        is MvModule -> MODULES
        is MvFunctionLike -> FUNCTIONS
        is MvTypeParameter -> TYPES
        is MvStruct -> TYPES
        is MvEnum -> ENUMS
        is MvEnumVariant -> ENUM_VARIANTS
        is MvPatBinding -> NAMES
        is MvFieldDecl -> NAMES
        is MvConst -> NAMES
        is MvSchema -> SCHEMAS
        is MvGlobalVariableStmt -> NAMES
        is MvLabelDecl -> NONE
        else -> error("when should be exhaustive, $this is not covered")
    }

fun List<ScopeEntry>.filterByNs(ns: NsSet): List<ScopeEntry> {
    return this.filter {
        it.namespaces.intersects(ns)
    }

}