package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor

class TyFunction(
    val item: MvFunctionLike,
    val typeVars: List<TyInfer.TyVar>,
    val paramTypes: List<Ty>,
    val retType: Ty,
    val acquiresTypes: List<Ty>,
    val typeArgs: List<Ty>,
) : Ty {
    override fun innerFoldWith(folder: TypeFolder): Ty {
        return TyFunction(
            item,
            typeVars,
            paramTypes.map { it.foldWith(folder) },
            retType.foldWith(folder),
            acquiresTypes.map { it.foldWith(folder) },
            typeArgs.map(folder)
        )
    }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        paramTypes.any { it.visitWith(visitor) } || retType.visitWith(visitor)

    override fun abilities(): Set<Ability> = Ability.all()

    override fun toString(): String = tyToString(this)
}
