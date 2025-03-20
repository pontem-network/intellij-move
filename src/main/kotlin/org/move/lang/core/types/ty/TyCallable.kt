package org.move.lang.core.types.ty

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import org.move.lang.core.psi.MvFunctionLike
import org.move.lang.core.psi.MvGenericDeclaration
import org.move.lang.core.psi.ext.returnTypeTy
import org.move.lang.core.psi.parameters
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.psi.typeParamsSubst
import org.move.lang.core.types.infer.*
import org.move.utils.PsiCachedValueProvider
import org.move.utils.getResults
import org.move.utils.moveStructureCacheResult

sealed class CallKind: TypeFoldable<CallKind> {
    open val flags: TypeFlags = 0

    data object Lambda: CallKind() {
        override fun deepFoldWith(folder: TypeFolder): CallKind = this
        override fun deepVisitWith(visitor: TypeVisitor): Boolean = false
    }

    data class GenericItem(val item: MvGenericDeclaration, val substitution: Substitution): CallKind() {
        override val flags: TypeFlags
            get() = mergeFlags(substitution.valueTys)

        override fun deepFoldWith(folder: TypeFolder): CallKind {
            return GenericItem(item, substitution.foldWith(folder))
        }

        override fun deepVisitWith(visitor: TypeVisitor): Boolean = substitution.visitWith(visitor)

        companion object {
            fun fake(project: Project): GenericItem {
                val fakeFunction = project.psiFactory.function("fun __fake()")
                return GenericItem(fakeFunction, emptySubstitution)
            }
        }
    }
}

data class TyCallable(
    val paramTypes: List<Ty>,
    val returnType: Ty,
    val kind: CallKind,
): Ty(mergeFlags(paramTypes) or returnType.flags or kind.flags) {

    fun genericKind(): CallKind.GenericItem? = this.kind as? CallKind.GenericItem

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


// does not account for explicit type arguments
fun MvFunctionLike.callableTy(msl: Boolean): TyCallable {
    val ty = if (!msl)
        GetTyCallable(this).getResults()
    else
        GetTyCallableMsl(this).getResults()
    return ty
}

private fun rawCallableTy(item: MvFunctionLike, msl: Boolean): TyCallable {
    val paramTypes = item.parameters.map { it.type?.loweredType(msl) ?: TyUnknown }
    val retType = item.returnTypeTy(msl)
    val kind = CallKind.GenericItem(item, item.typeParamsSubst)
    return TyCallable(paramTypes, retType, kind)
}

class GetTyCallable(override val owner: MvFunctionLike): PsiCachedValueProvider<TyCallable> {
    override fun compute(): CachedValueProvider.Result<TyCallable> {
        val ty = rawCallableTy(owner, false)
        return owner.project.moveStructureCacheResult(ty)
    }
}

class GetTyCallableMsl(override val owner: MvFunctionLike): PsiCachedValueProvider<TyCallable> {
    override fun compute(): CachedValueProvider.Result<TyCallable> {
        val ty = rawCallableTy(owner, true)
        return owner.project.moveStructureCacheResult(ty)
    }
}
