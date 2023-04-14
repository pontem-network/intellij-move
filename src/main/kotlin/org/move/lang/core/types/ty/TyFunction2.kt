package org.move.lang.core.types.ty

import com.intellij.openapi.project.Project
import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.types.infer.*

data class TyFunction2(
    override val item: MvFunctionLike,
    override val substitution: Substitution,
    val paramTypes: List<Ty>,
    val acquiresTypes: List<Ty>,
    val retType: Ty,
) : GenericTy(
    item,
    substitution,
    mergeFlags(paramTypes) or retType.flags or mergeFlags(acquiresTypes)
) {

    override fun innerFoldWith(folder: TypeFolder): Ty {
        return TyFunction2(
            item,
            substitution.foldValues(folder),
            paramTypes.map { it.foldWith(folder) },
            acquiresTypes.map { it.foldWith(folder) },
            retType.foldWith(folder),
        )
    }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        substitution.visitValues(visitor)
                || paramTypes.any { it.visitWith(visitor) }
                || retType.visitWith(visitor)
                || acquiresTypes.any { it.visitWith(visitor) }

    override fun abilities(): Set<Ability> = Ability.all()

    override fun toString(): String = tyToString(this)

    companion object {
        fun unknownTyFunction(project: Project, numParams: Int): TyFunction2 {
            val fakeFunction = project.psiFactory.function("fun __fake()")
            return TyFunction2(
                fakeFunction,
                emptySubstitution,
                generateSequence { TyUnknown }.take(numParams).toList(),
                emptyList(),
                TyUnknown
            )
        }
    }
}
