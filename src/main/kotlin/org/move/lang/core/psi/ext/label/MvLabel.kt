package org.move.lang.core.psi.ext.label

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvElementImpl
import org.move.lang.core.psi.MvLabel
import org.move.lang.core.psi.MvLabelDecl
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.scopeEntry.filterByName
import org.move.lang.core.resolve.getLabelResolveVariants
import org.move.lang.core.resolve.ref.MvPolyVariantReference
import org.move.lang.core.resolve.ref.MvPolyVariantReferenceCached
import org.move.lang.core.resolve.ref.ResolveCacheDependency
import org.move.lang.core.resolve.scopeEntry.namedElements

class MvLabelReferenceImpl(
    element: MvLabel
): MvPolyVariantReferenceCached<MvLabel>(element) {

    // todo: change to LOCAL
    override val cacheDependency: ResolveCacheDependency get() = ResolveCacheDependency.ANY_PSI_CHANGE

    override fun multiResolveInner(): List<MvNamedElement> {
        return getLabelResolveVariants(element)
            .filterByName(element.referenceName).namedElements()

    }

    override fun isReferenceTo(element: PsiElement): Boolean =
        element is MvLabelDecl && super.isReferenceTo(element)
}

abstract class MvLabelMixin(node: ASTNode): MvElementImpl(node), MvLabel {

    override val identifier: PsiElement get() = quoteIdentifier

    override val referenceNameElement: PsiElement get() = quoteIdentifier

    override fun getReference(): MvPolyVariantReference = MvLabelReferenceImpl(this)
}
