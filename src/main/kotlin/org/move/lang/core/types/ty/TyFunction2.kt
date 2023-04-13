package org.move.lang.core.types.ty

import org.move.ide.presentation.tyToString
import org.move.lang.core.types.infer.TypeFolder
import org.move.lang.core.types.infer.TypeVisitor
import org.move.lang.core.types.infer.mergeFlags

data class TyFunction2(
    val paramTypes: List<Ty>,
    val acquiresTypes: List<Ty>,
    val retType: Ty,
) : Ty(mergeFlags(paramTypes) or retType.flags or mergeFlags(acquiresTypes)) {

    override fun innerFoldWith(folder: TypeFolder): Ty {
        return TyFunction2(
            paramTypes.map { it.foldWith(folder) },
            acquiresTypes.map { it.foldWith(folder) },
            retType.foldWith(folder),
        )
    }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        paramTypes.any { it.visitWith(visitor) }
                || retType.visitWith(visitor)
                || acquiresTypes.any { it.visitWith(visitor) }

    override fun abilities(): Set<Ability> = Ability.all()

    override fun toString(): String = tyToString(this)
}
