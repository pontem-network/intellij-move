package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvMethodCall
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.types.infer.inference
import org.move.stdext.wrapWithList

class MvMethodCallReferenceImpl(
    element: MvMethodCall
): MvPolyVariantReferenceBase<MvMethodCall>(element) {

    override fun multiResolve(): List<MvNamedElement> {
        val msl = element.isMsl()
        val inference = element.inference(msl) ?: return emptyList()
        return inference.getResolvedMethod(element).wrapWithList()
    }

    override fun isReferenceTo(element: PsiElement): Boolean =
        element is MvFunction && super.isReferenceTo(element)
}
