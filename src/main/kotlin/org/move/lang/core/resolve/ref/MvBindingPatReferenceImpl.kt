package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElement
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.ancestorStrict
import org.move.lang.core.psi.ext.contains
import org.move.lang.core.psi.ext.isMsl
import org.move.lang.core.resolve.ScopeEntry
import org.move.lang.core.resolve.filterByName
import org.move.lang.core.resolve.getPatBindingsResolveVariants
import org.move.lang.core.types.infer.InferenceResult
import org.move.lang.core.types.infer.inference
import org.move.lang.core.types.ty.Ty
import org.move.stdext.wrapWithList

class MvBindingPatReferenceImpl(
    element: MvPatBinding
): MvPolyVariantReferenceBase<MvPatBinding>(element) {

    override fun multiResolve(): List<MvNamedElement> =
        rawMultiResolveUsingInferenceCache() ?: rawCachedMultiResolve()

    private fun rawMultiResolveUsingInferenceCache(): List<MvNamedElement>? {
        var inference: InferenceResult? = null
        // determine that binding is inside the left side of the match arm
        val matchArm = element.ancestorStrict<MvMatchArm>()
        if (matchArm != null && matchArm.pat.contains(element)) {
            // inside the lhs of match arm
            inference = matchArm.pat.inference(matchArm.pat.isMsl())
        }
        if (inference == null) {
            val letStmtPat = element.ancestorStrict<MvLetStmt>()?.pat
            if (letStmtPat != null && letStmtPat.contains(element)) {
                inference = letStmtPat.inference(letStmtPat.isMsl());
            }
        }
        if (inference == null) return null
        return inference.getResolvedPatBinding(element).wrapWithList()
    }

    private fun rawCachedMultiResolve(): List<MvNamedElement> =
        resolvePatBindingRaw(element, expectedType = null).map { it.element }

    override fun handleElementRename(newName: String): PsiElement {
        if (element.parent !is MvPatField) return super.handleElementRename(newName)
        val newFieldPat = element.project.psiFactory.fieldPatFull(newName, element.text)
        return element.replace(newFieldPat)
    }
}

fun resolvePatBindingRaw(binding: MvPatBinding, expectedType: Ty? = null): List<ScopeEntry> {
    val entries = getPatBindingsResolveVariants(binding, false)
        .filterEntriesByExpectedType(expectedType)

    return entries
        .filterByName(binding.referenceName)
}
