package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.MvMethodCall
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.resolve.ScopeEntry
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

interface DotExprResolveVariant : ScopeEntry {
    /** The receiver type after possible derefs performed */
//    val selfTy: Ty
    /** The number of `*` dereferences should be performed on receiver to match `selfTy` */
//    val derefCount: Int

    override val namespaces: Set<Namespace>
        get() = NAMES // Namespace does not matter in the case of dot expression

    override fun doCopyWithNs(namespaces: Set<Namespace>): ScopeEntry = this
}

data class FieldResolveVariant(
    override val name: String,
    override val element: MvNamedElement,
//    override val selfTy: Ty,
//    val derefSteps: List<Autoderef.AutoderefStep>,
//    val obligations: List<Obligation>,
): DotExprResolveVariant