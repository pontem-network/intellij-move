package org.move.lang.core.types.ty

import org.move.lang.core.psi.MvTypeParametersOwner
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.HAS_TY_INFER_VISITOR
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.HAS_TY_STRUCT_VISITOR
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.HAS_TY_TYPE_PARAMETER_VISITOR
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.HAS_TY_UNKNOWN_VISITOR
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.NEEDS_INFER
import org.move.lang.core.types.infer.HasTypeFlagVisitor.Companion.NEEDS_SUBST

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

val TypeFoldable<*>.hasTyInfer get() = visitWith(HAS_TY_INFER_VISITOR)
val TypeFoldable<*>.hasTyTypeParameters get() = visitWith(HAS_TY_TYPE_PARAMETER_VISITOR)
val TypeFoldable<*>.hasTyStruct get() = visitWith(HAS_TY_STRUCT_VISITOR)
val TypeFoldable<*>.hasTyUnknown get() = visitWith(HAS_TY_UNKNOWN_VISITOR)

val TypeFoldable<*>.needsInfer get(): Boolean = visitWith(NEEDS_INFER)
val TypeFoldable<*>.needsSubst get(): Boolean = visitWith(NEEDS_SUBST)

abstract class Ty(val flags: TypeFlags = 0) : TypeFoldable<Ty> {

    override fun foldWith(folder: TypeFolder): Ty = folder(this)

    override fun innerFoldWith(folder: TypeFolder): Ty = this

    override fun visitWith(visitor: TypeVisitor): Boolean = visitor(this)

    override fun innerVisitWith(visitor: TypeVisitor): Boolean = false

    /**
     * Bindings between formal type parameters and actual type arguments.
     */
    open val typeParameterValues: Substitution get() = emptySubstitution

    fun deref(): Ty = if (this is TyReference) this.referenced else TyUnknown

    /**
     * User visible string representation of a type
     */
    abstract override fun toString(): String

    abstract fun abilities(): Set<Ability>
}

//val Ty.isTypeParam: Boolean get() = this is TyInfer || this is TyTypeParameter

//fun Ty.mslTy(msl: Boolean): Ty = if (this is TyReference && msl) this.innermostTy() else this

fun Ty.mslScopeRefined(msl: Boolean): Ty {
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
    open val item: MvTypeParametersOwner,
    open val substitution: Substitution,
    flags: TypeFlags,
) : Ty(mergeFlags(substitution.types) or flags)
