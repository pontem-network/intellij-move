package org.move.lang.core.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceBase
import org.move.lang.core.types.infer.inference
import org.move.stdext.wrapWithList

// todo: change into VisibilityFilter
fun isFieldsAccessible(
    element: MvElement,
    item: MvStructOrEnumItemElement,
    msl: Boolean
): Boolean {
    if (!msl) {
        // cannot resolve field if not in the same module as struct definition
        val dotExprModule = element.namespaceModule ?: return false
        if (item.containingModule != dotExprModule) return false
    }
    return true
}

class MvFieldLookupReferenceImpl(
    element: MvFieldLookup
): MvPolyVariantReferenceBase<MvFieldLookup>(element) {

    override fun multiResolve(): List<MvNamedElement> {
        val msl = element.isMsl()
        val receiverExpr = element.receiverExpr
        val inference = receiverExpr.inference(msl) ?: return emptyList()
        return inference.getResolvedField(element).wrapWithList()
    }
}

abstract class MvFieldLookupMixin(node: ASTNode): MvElementImpl(node),
                                                  MvFieldLookup {

    override val referenceNameElement: PsiElement get() = (identifier ?: integerLiteral)!!

    override fun getReference(): MvPolyVariantReference = MvFieldLookupReferenceImpl(this)
}
