package org.move.lang.core.types.infer

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.elementType
import org.move.lang.core.types.ty.Ty
import org.move.lang.core.types.ty.TyInfer
import org.move.lang.core.types.ty.TyTypeParameter

fun inferExpectedTy(element: PsiElement, inference: InferenceResult): Ty? {
    val owner = element.parent
    return when {
        owner is MvTypeArgument -> inferExpectedTypeArgumentTy(owner)
        element is MvExpr -> inference.getExpectedType(element)
        else -> null
    }
}

fun inferExpectedTypeArgumentTy(typeArgument: MvTypeArgument): Ty? {
    val parent = typeArgument.parent
    return when (parent) {
        is MvVectorLitExpr -> TyInfer.TyVar()
        is MvTypeArgumentList -> {
            val typeArgumentList = typeArgument.parent as MvTypeArgumentList
            val paramIndex =
                typeArgumentList.children.indexOfFirst { it.textRange.contains(typeArgument.textOffset) }
            if (paramIndex == -1) return null

            val path = typeArgumentList.parent as MvPath
            val genericItem = path.reference?.resolveWithAliases() as? MvTypeParametersOwner ?: return null
            genericItem.typeParameters
                .getOrNull(paramIndex)
                ?.let { TyInfer.TyVar(TyTypeParameter(it)) }
        }
        else -> error("invalid MvTypeArgument parent ${parent.elementType}")
    }
}
