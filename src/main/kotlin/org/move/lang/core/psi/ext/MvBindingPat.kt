package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.move.ide.MoveIcons
import org.move.lang.core.psi.*
import org.move.lang.core.psi.impl.MvNameIdentifierOwnerImpl
import org.move.lang.core.types.infer.*
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyUnknown
import javax.swing.Icon

val MvBindingPat.owner: PsiElement?
    get() = PsiTreeUtil.findFirstParent(this) {
        it is MvLetStmt
                || it is MvFunctionParameter
//                || it is MvConst
                || it is MvSchemaFieldStmt
    }

fun MvBindingPat.inferBindingTy(parentCtx: InferenceContext, itemContext: ItemContext): Ty {
    val existingTy = parentCtx.bindingTypes[this]
    if (existingTy != null) {
        return existingTy
    }
    val owner = this.owner
    return when (owner) {
        is MvFunctionParameter -> owner.paramTypeTy(itemContext)
//        is MvConst -> owner.constAnnotationTy(itemContext)
        is MvLetStmt -> {
            if (parentCtx.bindingTypes.containsKey(this)) return parentCtx.bindingTypes[this]!!

            val pat = owner.pat ?: return TyUnknown
            val explicitType = owner.typeAnnotation?.type
            if (explicitType != null) {
                val explicitTy = itemContext.getTypeTy(explicitType)
                collectBindings(pat, explicitTy, parentCtx)
                return parentCtx.bindingTypes[this] ?: TyUnknown
            }
            val inferredTy = owner.initializer?.expr?.let { inferExprTy(it, parentCtx) } ?: TyUnknown
            collectBindings(pat, inferredTy, parentCtx)
            return parentCtx.bindingTypes[this] ?: TyUnknown
        }
        is MvSchemaFieldStmt -> owner.annotationTy(itemContext)
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
        return when (this.owner) {
            is MvFunctionParameter -> {
                val function = this.ancestorStrict<MvFunction>() ?: return super.getUseScope()
                var combinedScope: SearchScope = LocalSearchScope(function)
                for (itemSpec in function.innerItemSpecs()) {
                    combinedScope = combinedScope.union(LocalSearchScope(itemSpec))
                }
                for (itemSpec in function.outerItemSpecs()) {
                    combinedScope = combinedScope.union(LocalSearchScope(itemSpec))
                }
                combinedScope
            }
            is MvLetStmt -> {
                val function = this.ancestorStrict<MvFunction>() ?: return super.getUseScope()
                LocalSearchScope(function)
            }
            is MvSchemaFieldStmt -> super.getUseScope()
            is MvConst -> super.getUseScope()
            else -> super.getUseScope()
        }
    }
}
