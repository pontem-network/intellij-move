package org.move.lang.core.types.ty

import org.move.lang.core.psi.MvGenericDeclaration
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.HAS_TY_ADT_VISITOR
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.HAS_TY_INFER_VISITOR
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.HAS_TY_TYPE_PARAMETER_VISITOR
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.HAS_TY_UNKNOWN_VISITOR
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.NEEDS_SUBST
import org.move.lang.core.types.ty.Ability.COPY

enum class Ability {
    DROP, COPY, STORE, KEY;

    fun label(): String = this.name.lowercase()

    fun requires(): Ability {
        return when (this) {
            DROP -> DROP
            COPY -> COPY
            KEY, STORE -> STORE
        }
    }

    override fun toString(): String {
        return super.toString().lowercase()
    }

    companion object {
        fun none(): Set<Ability> = setOf()
        fun all(): Set<Ability> = setOf(DROP, COPY, STORE, KEY)
    }
}

val Ty.isCopy: Boolean get() = this.abilities().contains(COPY)

val TypeFoldable<*>.hasTyInfer get() = visitWith(HAS_TY_INFER_VISITOR)
val TypeFoldable<*>.hasTyTypeParameters get() = visitWith(HAS_TY_TYPE_PARAMETER_VISITOR)
val TypeFoldable<*>.hasTyAdt get() = visitWith(HAS_TY_ADT_VISITOR)
val TypeFoldable<*>.hasTyUnknown get() = visitWith(HAS_TY_UNKNOWN_VISITOR)

//val TypeFoldable<*>.needsInfer get(): Boolean = visitWith(NEEDS_INFER)
val TypeFoldable<*>.needsSubst get(): Boolean = visitWith(NEEDS_SUBST)

//fun Ty.knownOrNull(): Ty? = takeIf { it !is TyUnknown }

abstract class Ty(val flags: TypeFlags = 0) : TypeFoldable<Ty> {

    override fun foldWith(folder: TypeFolder): Ty = folder.fold(this)

    override fun deepFoldWith(folder: TypeFolder): Ty = this

    override fun visitWith(visitor: TypeVisitor): Boolean = visitor.visit(this)

    override fun deepVisitWith(visitor: TypeVisitor): Boolean = false

    /**
     * Bindings between formal type parameters and actual type arguments.
     */
    open val typeParameterValues: Substitution get() = emptySubstitution

    fun derefIfNeeded(): Ty = if (this is TyReference) this.referenced.derefIfNeeded() else this

    /**
     * User visible string representation of a type
     */
    abstract override fun toString(): String

    abstract fun abilities(): Set<Ability>
}

fun Ty.refineForSpecs(msl: Boolean): Ty {
    var ty = this
    if (!msl) return ty

    if (this is TyReference) {
        ty = this.innermostTy()
    }
    if (ty is TyInteger || ty is TyInfer.IntVar) {
        ty = TyNum
    }
    return ty
}

abstract class GenericTy(
    open val item: MvGenericDeclaration,
    open val substitution: Substitution,
    flags: TypeFlags,
) : Ty(mergeFlags(substitution.types) or flags)
