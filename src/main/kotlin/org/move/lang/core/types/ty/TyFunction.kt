package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.types.infer.TypeFolder

class TyFunction(
    val item: MvFunctionLike,
    val typeVars: List<TyInfer.TyVar>,
    val paramTypes: List<Ty>,
    val retType: Ty,
    val acquiresTypes: List<Ty>,
) : Ty {
    var solvable: Boolean = true

    override fun innerFoldWith(folder: TypeFolder): Ty {
        return TyFunction(
            item,
            typeVars,
            paramTypes.map { it.foldWith(folder) },
            retType.foldWith(folder),
            acquiresTypes.map { it.foldWith(folder) }
        )
    }

    override fun abilities(): Set<Ability> = Ability.all()

    override fun toString(): String = tyToString(this)
}
