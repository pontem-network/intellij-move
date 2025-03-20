/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.lang.core.types.infer

import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyTypeParameter

abstract class TypeFolder {
//    val cache = mutableMapOf<Ty, Ty>()
//    var foldDepth = 0

//    operator fun invoke(ty: Ty): Ty {
//        // workaround for recursive structs, folding gives up at some point
////        if (foldDepth > MAX_RECURSION_DEPTH) return TyUnknown
//        return fold(ty)
////        val cachedTy = cache[ty]
////        if (cachedTy != null) {
////            return cachedTy
////        } else {
////            val foldedTy = fold(ty)
////            cache[ty] = foldedTy
////            return foldedTy
////        }
//    }

    abstract fun fold(ty: Ty): Ty

//    companion object {
//        const val MAX_RECURSION_DEPTH = 25
//    }
}

//typealias TypeVisitor = (Ty) -> Boolean

abstract class TypeVisitor {
    abstract fun visit(ty: Ty): Boolean
}

/**
 * Despite a scary name, [TypeFoldable] is a rather simple thing.
 *
 * It allows to map type variables within a type to other types.
 */
interface TypeFoldable<out Self> {
    /**
     * Fold `this` type with the folder.
     *
     * This works for:
     * ```
     *     A.foldWith { C } == C
     *     A<B>.foldWith { C } == C
     * ```
     *
     * `a.foldWith(folder)` is equivalent to `folder(a)` in cases where `a` is `Ty`.
     * In other cases the call delegates to [deepFoldWith]
     *
     * The folding basically is not deep. If you want to fold type deeply, you should write a folder
     * somehow like this:
     * ```kotlin
     * // We initially have `ty = A<B<C>, B<C>>` and want replace C to D to get `A<B<D>, B<D>>`
     * ty.foldWith(object : TypeFolder {
     *     override fun invoke(ty: Ty): Ty =
     *         if (it == C) D else it.superFoldWith(this)
     * })
     * ```
     */
    fun foldWith(folder: TypeFolder): Self = deepFoldWith(folder)

    /**
     * Fold inner types (not this type) with the folder.
     * `A<A<B>>.foldWith { C } == A<C>`
     * This method should be used only by a folder implementations internally.
     */
    fun deepFoldWith(folder: TypeFolder): Self

    /** Similar to [foldWith], but just visit types without folding */
    fun visitWith(visitor: TypeVisitor): Boolean = deepVisitWith(visitor)

    /** Similar to [deepFoldWith], but just visit types without folding */
    fun deepVisitWith(visitor: TypeVisitor): Boolean
}

/** Deeply replace any [TyInfer] with the function [folder] */
fun <T> TypeFoldable<T>.foldTyInferWith(folder: (TyInfer) -> Ty): T {
    val folder = object : TypeFolder() {
        override fun fold(ty: Ty): Ty {
            val foldedTy = if (ty is TyInfer) folder(ty) else ty
            return foldedTy.deepFoldWith(this)
        }
    }
    return foldWith(folder)
}

/** Deeply replace any [TyTypeParameter] with the function [folder] */
fun <T> TypeFoldable<T>.deepFoldTyTypeParameterWith(folder: (TyTypeParameter) -> Ty): T =
    foldWith(object : TypeFolder() {
        override fun fold(ty: Ty): Ty =
            if (ty is TyTypeParameter) folder(ty) else ty.deepFoldWith(this)
    })
//

//fun <T> TypeFoldable<T>.visitTyTypeParameterWith(visitor: (TyTypeParameter) -> Boolean) =
//    visitWith(object : TypeVisitor {
//        override fun invoke(ty: Ty): Boolean =
//            if (ty is TyTypeParameter) visitor(ty) else ty.deepVisitWith(this)
//    })

//fun <T> TypeFoldable<T>.visitTyVarWith(visitor: (TyInfer.TyVar) -> Boolean) =
//    visitWith(object : TypeVisitor() {
//        override fun visit(ty: Ty): Boolean =
//            if (ty is TyInfer.TyVar) visitor(ty) else ty.deepVisitWith(this)
//    })

//fun <T> TypeFoldable<T>.containsTyOfClass(classes: List<Class<*>>): Boolean =
//    visitWith(object : TypeVisitor() {
//        override fun visit(ty: Ty): Boolean =
//            if (classes.any { it.isInstance(ty) }) true else ty.deepVisitWith(this)
//    })
