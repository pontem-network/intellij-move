package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.infer.mergeFlags

interface TyCallable {
    val paramTypes: List<Ty>
    val returnType: Ty
}

data class TyLambda(
    override val paramTypes: List<Ty>,
    override val returnType: Ty
) : Ty(mergeFlags(paramTypes) or returnType.flags), TyCallable {

    override fun abilities(): Set<Ability> = emptySet()

    override fun toString(): String = tyToString(this)

    override fun deepFoldWith(folder: TypeFolder): Ty {
        return TyLambda(
            paramTypes.map { it.foldWith(folder) },
            returnType.foldWith(folder),
        )
    }

    override fun deepVisitWith(visitor: TypeVisitor): Boolean =
        paramTypes.any { it.visitWith(visitor) } || returnType.visitWith(visitor)

    companion object {
        fun unknown(numParams: Int): TyLambda {
            return TyLambda(generateSequence { TyUnknown }.take(numParams).toList(), TyUnknown)
        }
    }
}
