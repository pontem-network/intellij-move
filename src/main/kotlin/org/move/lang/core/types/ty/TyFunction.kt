package org.move.lang.core.types.ty

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import org.move.ide.presentation.tyToString
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.returnTypeTy
import org.move.lang.core.types.infer.*
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
import org.move.utils.moveStructureCacheResult

data class TyFunction(
    val item: MvGenericDeclaration,
    val substitution: Substitution,
    override val paramTypes: List<Ty>,
    override val returnType: Ty,
): TyCallable, Ty(
    mergeFlags(substitution.valueTys) or mergeFlags(paramTypes) or returnType.flags
) {

    override fun deepFoldWith(folder: TypeFolder): Ty {
        return TyFunction(
            item,
            substitution.foldWith(folder),
            paramTypes.map { it.foldWith(folder) },
            returnType.foldWith(folder),
        )
    }

    override fun deepVisitWith(visitor: TypeVisitor): Boolean =
        substitution.visitWith(visitor)
                || paramTypes.any { it.visitWith(visitor) }
                || returnType.visitWith(visitor)

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
            )
        }
    }
}

// does not account for explicit type arguments
fun MvFunctionLike.functionTy(msl: Boolean): TyFunction {
    val ty = if (!msl)
        GetTyFunction(this).getResults()
    else
        GetTyFunctionMsl(this).getResults()
    return ty
}

class GetTyFunction(override val owner: MvFunctionLike): PsiCachedValueProvider<TyFunction> {
    override fun compute(): CachedValueProvider.Result<TyFunction> {
        val ty = rawFunctionTy(owner, false)
        return owner.project.moveStructureCacheResult(ty)
    }
}

class GetTyFunctionMsl(override val owner: MvFunctionLike): PsiCachedValueProvider<TyFunction> {
    override fun compute(): CachedValueProvider.Result<TyFunction> {
        val ty = rawFunctionTy(owner, true)
        return owner.project.moveStructureCacheResult(ty)
    }
}

private fun rawFunctionTy(item: MvFunctionLike, msl: Boolean): TyFunction {
    val typeParamsSubst = item.typeParamsSubst
    val paramTypes = item.parameters.map { it.type?.loweredType(msl) ?: TyUnknown }
    val retType = item.returnTypeTy(msl)
    return TyFunction(
        item,
        typeParamsSubst,
        paramTypes,
        retType,
    )
}

