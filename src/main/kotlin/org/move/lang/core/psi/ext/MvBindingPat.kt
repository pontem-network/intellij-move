package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.mixins.declaredTy
import org.move.lang.core.types.infer.InferenceContext
import org.move.lang.core.types.infer.collectBindings
import org.move.lang.core.types.infer.inferExprTy
import org.move.lang.core.types.infer.inferTypeTy
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MvBindingPat.owner: PsiElement?
    get() = PsiTreeUtil.findFirstParent(this) {
        it is MvLetStmt
                || it is MvFunctionParameter
                || it is MvConst
                || it is MvSchemaFieldStmt
    }

fun MvBindingPat.declaredTy(parentCtx: InferenceContext): Ty {
    val owner = this.owner
    return when (owner) {
        is MvFunctionParameter -> owner.declaredTy(parentCtx.msl)
        is MvConst -> owner.declaredTy(parentCtx.msl)
        is MvLetStmt -> {
            if (parentCtx.bindingTypes.containsKey(this)) return parentCtx.bindingTypes[this]!!

            val pat = owner.pat ?: return TyUnknown
            val explicitType = owner.typeAnnotation?.type
            if (explicitType != null) {
                val explicitTy = inferTypeTy(explicitType, parentCtx.msl)
                collectBindings(pat, explicitTy, parentCtx)
                return parentCtx.bindingTypes[this] ?: TyUnknown
            }
            return TyUnknown
        }
        is MvSchemaFieldStmt -> owner.declaredTy(parentCtx.msl)
        else -> TyUnknown
    }
}

fun MvBindingPat.inferredTy(parentCtx: InferenceContext): Ty {
    val owner = this.owner
    return when (owner) {
        is MvFunctionParameter -> owner.declaredTy(parentCtx.msl)
        is MvConst -> owner.declaredTy(parentCtx.msl)
        is MvLetStmt -> {
            if (parentCtx.bindingTypes.containsKey(this)) return parentCtx.bindingTypes[this]!!

            val pat = owner.pat ?: return TyUnknown
            val explicitType = owner.typeAnnotation?.type
            if (explicitType != null) {
                val explicitTy = inferTypeTy(explicitType, parentCtx.msl)
                collectBindings(pat, explicitTy, parentCtx)
                return parentCtx.bindingTypes[this] ?: TyUnknown
            }
            val inferredTy = owner.initializer?.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown
            collectBindings(pat, inferredTy, parentCtx)
            return parentCtx.bindingTypes[this] ?: TyUnknown
        }
        is MvSchemaFieldStmt -> owner.declaredTy(parentCtx.msl)
        else -> TyUnknown
    }
}

abstract class MvBindingPatMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                  MvBindingPat {
    override fun getIcon(flags: Int): Icon =
        when (this.owner) {
            is MvFunctionParameter -> MoveIcons.PARAMETER
            is MvConst -> MoveIcons.CONST
            else -> MoveIcons.VARIABLE
        }

    override fun getUseScope(): SearchScope {
        val function = this.ancestorStrict<MvFunction>() ?: return super.getUseScope()
        return LocalSearchScope(function)
    }
}
