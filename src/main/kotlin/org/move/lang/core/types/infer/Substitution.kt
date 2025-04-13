package org.move.lang.core.types.infer

import com.google.common.collect.Maps
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.types.ty.*
import org.move.stdext.zipValues

open class Substitution(val typeSubst: Map<TyTypeParameter, Ty> = emptyMap()) : TypeFoldable<Substitution> {

    val types: Collection<Ty> get() = typeSubst.values

    operator fun plus(other: Substitution): Substitution =
        Substitution(mergeMaps(typeSubst, other.typeSubst))

    operator fun get(key: TyTypeParameter): Ty? = typeSubst[key]

    fun getPsi(psi: MvTypeParameter): Ty? = typeSubst[TyTypeParameter.named(psi)]
//    operator fun get(psi: MvTypeParameter): Ty? = typeSubst[TyTypeParameter.named(psi)]

//    fun typeParameterByName(name: String): TyTypeParameter? =
//        typeSubst.keys.find { it.toString() == name }

    fun substituteInValues(map: Substitution): Substitution =
        Substitution(
            typeSubst.mapValues { (_, value) -> value.substitute(map) },
        )

    fun foldValues(folder: TypeFolder): Substitution =
        Substitution(
            typeSubst.mapValues { (_, value) -> value.foldWith(folder) },
        )

    override fun innerFoldWith(folder: TypeFolder): Substitution = foldValues(folder)

    override fun innerVisitWith(visitor: TypeVisitor): Boolean {
        return typeSubst.values.any { it.visitWith(visitor) }
    }

    fun zipTypeValues(other: Substitution): List<Pair<Ty, Ty>> = zipValues(typeSubst, other.typeSubst)

    fun mapTypeKeys(transform: (Map.Entry<TyTypeParameter, Ty>) -> TyTypeParameter): Substitution =
        Substitution(typeSubst.mapKeys(transform))

    fun mapTypeValues(transform: (Map.Entry<TyTypeParameter, Ty>) -> Ty): Substitution =
        Substitution(typeSubst.mapValues(transform))

    fun visitValues(visitor: TypeVisitor): Boolean = types.any { it.visitWith(visitor) }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        other !is Substitution -> false
        typeSubst != other.typeSubst -> false
        else -> true
    }

    override fun hashCode(): Int = typeSubst.hashCode()
}

private object EmptySubstitution : Substitution()

val emptySubstitution: Substitution = EmptySubstitution

/**
 * Deeply replace any [TyTypeParameter] by [subst] mapping.
 */
fun <T : TypeFoldable<T>> T.substitute(subst: Substitution): T =
    foldWith(object : TypeFolder() {
        override fun fold(ty: Ty): Ty {
            return when {
                ty is TyTypeParameter -> subst[ty] ?: ty
                ty.needsSubst -> ty.innerFoldWith(this)
                else -> ty
            }
        }
    })

fun <T : TypeFoldable<T>> TypeFoldable<T>.substituteOrUnknown(subst: Substitution): T =
    foldWith(object : TypeFolder() {
        override fun fold(ty: Ty): Ty = when {
            ty is TyTypeParameter -> subst[ty] ?: TyUnknown
            ty.needsSubst -> ty.innerFoldWith(this)
            else -> ty
        }
    })

fun <T> TypeFoldable<T>.visitTyInfers(visitor: (TyInfer) -> Boolean): Boolean {
    return visitWith(object : TypeVisitor {
        override fun invoke(ty: Ty): Boolean = when {
            ty is TyInfer -> visitor(ty)
            ty.hasTyInfer -> ty.innerVisitWith(this)
            else -> false
        }
    })
}

private fun <K, V> mergeMaps(map1: Map<K, V>, map2: Map<K, V>): Map<K, V> =
    when {
        map1.isEmpty() -> map2
        map2.isEmpty() -> map1
        else -> Maps.newHashMapWithExpectedSize<K, V>(map1.size + map2.size).apply {
            putAll(map1)
            putAll(map2)
        }
    }
