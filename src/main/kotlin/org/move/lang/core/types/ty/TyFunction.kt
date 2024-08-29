package org.move.lang.core.types.ty

import com.intellij.openapi.project.Project
import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.psi.MvGenericDeclaration
import org.move.lang.core.psi.acquiresPathTypes
import org.move.lang.core.psi.ext.returnTypeTy
import org.move.lang.core.psi.parameters
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.psi.typeParamsToTypeParamsSubst
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.infer.loweredType

data class TyFunction(
    override val item: MvGenericDeclaration,
    override val substitution: Substitution,
    override val paramTypes: List<Ty>,
    override val returnType: Ty,
    val acquiresTypes: List<Ty>,
): TyCallable, GenericTy(
    item,
    substitution,
    mergeFlags(paramTypes) or mergeFlags(acquiresTypes) or returnType.flags
) {

    fun needsTypeAnnotation(): Boolean = this.substitution.hasTyInfer

    override fun innerFoldWith(folder: TypeFolder): Ty {
        return TyFunction(
            item,
            substitution.foldValues(folder),
            paramTypes.map { it.foldWith(folder) },
            returnType.foldWith(folder),
            acquiresTypes.map { it.foldWith(folder) },
        )
    }

    override fun innerVisitWith(visitor: TypeVisitor): Boolean =
        substitution.visitValues(visitor)
                || paramTypes.any { it.visitWith(visitor) }
                || returnType.visitWith(visitor)
                || acquiresTypes.any { it.visitWith(visitor) }

    override fun abilities(): Set<Ability> = Ability.all()

    override fun toString(): String = tyToString(this)

    companion object {
        fun unknownTyFunction(project: Project, numParams: Int): TyFunction {
            val fakeFunction = project.psiFactory.function("fun __fake()")
            return TyFunction(
                fakeFunction,
                emptySubstitution,
                generateSequence { TyUnknown }.take(numParams).toList(),
                TyUnknown,
                acquiresTypes = emptyList(),
            )
        }
    }
}

fun MvFunctionLike.functionTy(msl: Boolean): TyFunction = callableTy(this, msl)

fun callableTy(item: MvFunctionLike, msl: Boolean): TyFunction {
    val typeParameters = item.typeParamsToTypeParamsSubst
    val paramTypes = item.parameters.map { it.type?.loweredType(msl) ?: TyUnknown }
    val acquiredTypes = item.acquiresPathTypes.map { it.loweredType(msl) }
    val retType = item.returnTypeTy(msl)
    return TyFunction(
        item,
        typeParameters,
        paramTypes,
        retType,
        acquiredTypes,
    )
}
