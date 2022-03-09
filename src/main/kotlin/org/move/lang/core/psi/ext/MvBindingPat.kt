package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.MvIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.psi.mixins.declaredTy
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MvBindingPat.owner: PsiElement?
    get() = PsiTreeUtil.findFirstParent(this) {
        it is MvLetStatement
                || it is MvFunctionParameter || it is MvConstDef
    }

fun MvBindingPat.ty(msl: Boolean): Ty = inferBindingTy(this, msl)

fun MvBindingPat.cachedTy(ctx: InferenceContext): Ty {
    val owner = this.owner
    return when (owner) {
        is MvFunctionParameter -> owner.declaredTy(ctx.msl)
        is MvConstDef -> owner.declaredTy(ctx.msl)
        is MvLetStatement -> {
            if (ctx.bindingTypes.containsKey(this)) return ctx.bindingTypes[this]!!

            val pat = owner.pat ?: return TyUnknown
            val explicitType = owner.typeAnnotation?.type
            if (explicitType != null) {
                val explicitTy = inferMvTypeTy(explicitType, ctx.msl)
                return collectBindings(pat, explicitTy)[this] ?: TyUnknown
            }
            val inferredTy = owner.initializer?.expr?.let { inferExprTy(it, ctx) } ?: TyUnknown
            val bindings = collectBindings(pat, inferredTy)
            ctx.bindingTypes.putAll(bindings)
            return bindings[this] ?: TyUnknown
        }
        else -> TyUnknown
    }
}

abstract class MvBindingPatMixin(node: ASTNode) : MvNameIdentifierOwnerImpl(node),
                                                  MvBindingPat {

    override fun getIcon(flags: Int): Icon =
        when (this.owner) {
            is MvFunctionParameter -> MvIcons.PARAMETER
            is MvConstDef -> MvIcons.CONST
            else -> MvIcons.VARIABLE
        }
}
