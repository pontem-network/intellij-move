package org.move.lang.core.types.ty

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.returnTypeTy
import org.move.lang.core.types.infer.*
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
import org.move.utils.moveStructureCacheResult

data class TyCallable(
    val paramTypes: List<Ty>,
    val returnType: Ty,
    val kind: CallKind,
): Ty(mergeFlags(paramTypes) or returnType.flags or kind.flags) {

    fun genericKind(): CallKind.Function? = this.kind as? CallKind.Function

    override fun abilities(): Set<Ability> = Ability.all()

    override fun deepFoldWith(folder: TypeFolder): Ty =
        TyCallable(
            paramTypes.map { it.foldWith(folder) },
            returnType.foldWith(folder),
            kind.foldWith(folder),
        )

    override fun deepVisitWith(visitor: TypeVisitor): Boolean =
        kind.visitWith(visitor)
                || paramTypes.any { it.visitWith(visitor) }
                || returnType.visitWith(visitor)

    companion object {
        fun fake(numParams: Int, callKind: CallKind): TyCallable {
            val paramTypes = generateSequence { TyUnknown }.take(numParams).toList()
            val returnType = TyUnknown
            return TyCallable(paramTypes, returnType, callKind)
        }
    }
}

sealed class CallKind: TypeFoldable<CallKind> {
    open val flags: TypeFlags = 0

    data object Lambda: CallKind() {
        override fun deepFoldWith(folder: TypeFolder): CallKind = this
        override fun deepVisitWith(visitor: TypeVisitor): Boolean = false
    }

    data class Function(val item: MvGenericDeclaration, val substitution: Substitution): CallKind() {
        override val flags: TypeFlags
            get() = mergeFlags(substitution.valueTys)

        override fun deepFoldWith(folder: TypeFolder): CallKind {
            return Function(item, substitution.foldWith(folder))
        }

        override fun deepVisitWith(visitor: TypeVisitor): Boolean = substitution.visitWith(visitor)

        companion object {
            fun fake(project: Project): Function {
                val fakeFunction = project.psiFactory.function("fun __fake()")
                return Function(fakeFunction, emptySubstitution)
            }
        }
    }
}

// does not account for explicit type arguments
fun MvFunctionLike.functionTy(msl: Boolean): TyCallable {
    val ty = if (!msl)
        GetTyCallable(this).getResults()
    else
        GetTyCallableMsl(this).getResults()
    return ty
}

private fun rawFunctionTy(item: MvFunctionLike, msl: Boolean): TyCallable {
    val paramTypes = item.parameters.map { it.type?.loweredType(msl) ?: TyUnknown }
    val retType = item.returnTypeTy(msl)
    val kind = CallKind.Function(item, item.typeParamsSubst)
    return TyCallable(paramTypes, retType, kind)
}

class GetTyCallable(override val owner: MvFunctionLike): PsiCachedValueProvider<TyCallable> {
    override fun compute(): CachedValueProvider.Result<TyCallable> {
        val ty = rawFunctionTy(owner, false)
        return owner.project.moveStructureCacheResult(ty)
    }
}

class GetTyCallableMsl(override val owner: MvFunctionLike): PsiCachedValueProvider<TyCallable> {
    override fun compute(): CachedValueProvider.Result<TyCallable> {
        val ty = rawFunctionTy(owner, true)
        return owner.project.moveStructureCacheResult(ty)
    }
}
