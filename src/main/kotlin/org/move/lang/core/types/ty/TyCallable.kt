package org.move.lang.core.types.ty

import com.intellij.openapi.project.Project
import org.move.lang.core.psi.MvGenericDeclaration
import org.move.lang.core.psi.psiFactory
import org.move.lang.core.types.infer.*

sealed class CallableKind: TypeFoldable<CallableKind> {
    open val flags: TypeFlags = 0

    object Lambda: CallableKind() {
        override fun deepFoldWith(folder: TypeFolder): CallableKind = this
        override fun deepVisitWith(visitor: TypeVisitor): Boolean = false
    }

    data class Function(val item: MvGenericDeclaration, val substitution: Substitution): CallableKind() {
        override val flags: TypeFlags
            get() = mergeFlags(substitution.valueTys)

        override fun deepFoldWith(folder: TypeFolder): CallableKind {
            return Function(item, substitution.foldWith(folder))
        }

        override fun deepVisitWith(visitor: TypeVisitor): Boolean = substitution.visitWith(visitor)

        companion object {
            fun fake(project: Project): CallableKind {
                val fakeFunction = project.psiFactory.function("fun __fake()")
                return Function(fakeFunction, emptySubstitution)
            }
        }
    }
}

data class TyCallable2(
    val paramTypes: List<Ty>,
    val returnType: Ty,
    val kind: CallableKind,
): Ty(mergeFlags(paramTypes) or returnType.flags or kind.flags) {

    override fun abilities(): Set<Ability> = Ability.all()

    override fun deepFoldWith(folder: TypeFolder): Ty =
        TyCallable2(
            paramTypes.map { it.foldWith(folder) },
            returnType.foldWith(folder),
            kind.foldWith(folder),
        )

    override fun deepVisitWith(visitor: TypeVisitor): Boolean =
        kind.visitWith(visitor)
                || paramTypes.any { it.visitWith(visitor) }
                || returnType.visitWith(visitor)

    companion object {
        fun fake(numParams: Int, fakeKind: CallableKind): TyCallable2 {
            val paramTypes = generateSequence { TyUnknown }.take(numParams).toList()
            val returnType = TyUnknown
            return TyCallable2(paramTypes, returnType, fakeKind)
        }
    }
}