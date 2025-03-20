package org.move.lang.core.types.infer

import org.move.lang.core.types.ty.*

open class Substitution(val mapping: Map<TyTypeParameter, Ty>): TypeFoldable<Substitution> {

    val valueTys: Collection<Ty> get() = mapping.values

    operator fun get(key: TyTypeParameter): Ty? = mapping[key]

    override fun deepFoldWith(folder: TypeFolder): Substitution {
        return Substitution(
            mapping.mapValues { (_, value) -> value.foldWith(folder) },
        )
    }

    override fun deepVisitWith(visitor: TypeVisitor): Boolean = mapping.values.any { it.visitWith(visitor) }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        other !is Substitution -> false
        mapping != other.mapping -> false
        else -> true
    }

    override fun hashCode(): Int = mapping.hashCode()
}

private object EmptySubstitution: Substitution(emptyMap())

val emptySubstitution: Substitution = EmptySubstitution

/**
 * Deeply replace any [TyTypeParameter] by [subst] mapping.
 */
fun <T: TypeFoldable<T>> T.substitute(subst: Substitution): T =
    foldWith(object: TypeFolder() {
        override fun fold(ty: Ty): Ty {
            return when {
                ty is TyTypeParameter -> subst[ty] ?: ty
                ty.needsSubst -> ty.deepFoldWith(this)
                else -> ty
            }
        }
    })

fun <T: TypeFoldable<T>> TypeFoldable<T>.substituteOrUnknown(subst: Substitution): T =
    foldWith(object: TypeFolder() {
        override fun fold(ty: Ty): Ty = when {
            ty is TyTypeParameter -> subst[ty] ?: TyUnknown
            ty.needsSubst -> ty.deepFoldWith(this)
            else -> ty
        }
    })

fun <T> TypeFoldable<T>.deepVisitTyInfers(visitor: (TyInfer) -> Boolean): Boolean {
    return visitWith(object: TypeVisitor() {
        override fun visit(ty: Ty): Boolean = when {
            ty is TyInfer -> visitor(ty)
            ty.hasTyInfer -> ty.deepVisitWith(this)
            else -> false
        }
    })
}
