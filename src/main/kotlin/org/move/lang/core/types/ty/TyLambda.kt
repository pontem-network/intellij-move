package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.infer.mergeFlags

// TODO: inherit from GenericTy ?
interface TyCallable {
    val paramTypes: List<Ty>
    val retType: Ty
}

data class TyLambda(
    override val paramTypes: List<Ty>,
    override val retType: Ty
) : Ty(mergeFlags(paramTypes) or retType.flags), TyCallable {

    override fun abilities(): Set<Ability> = emptySet()

    override fun toString(): String = tyToString(this)

    override fun innerFoldWith(folder: TypeFolder): Ty {
        return TyLambda(
            paramTypes.map { it.foldWith(folder) },
            retType.foldWith(folder),
        )
    }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        paramTypes.any { it.visitWith(visitor) } || retType.visitWith(visitor)

    companion object {
        fun unknown(numParams: Int): TyLambda {
            return TyLambda(generateSequence { TyUnknown }.take(numParams).toList(), TyUnknown)
        }
    }
}
