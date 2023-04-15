package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.structLitExpr
import org.move.lang.core.types.ty.*

fun inferExpectedTy(element: PsiElement, inference: InferenceResult): Ty? {
    val owner = element.parent
    return when (owner) {
        is MvTypeArgument -> {
            val typeArgumentList = owner.parent as MvTypeArgumentList
            val paramIndex =
                typeArgumentList.children.indexOfFirst { it.textRange.contains(owner.textOffset) }
            if (paramIndex == -1) return null

            val path = typeArgumentList.parent as MvPath
            val genericItem = path.reference?.resolveWithAliases() as? MvTypeParametersOwner ?: return null
            genericItem.typeParameters
                .getOrNull(paramIndex)
                ?.let { TyInfer.TyVar(TyTypeParameter(it)) }
        }
        else -> if (element is MvExpr) inference.getExpectedType(element) else null
    }
}
