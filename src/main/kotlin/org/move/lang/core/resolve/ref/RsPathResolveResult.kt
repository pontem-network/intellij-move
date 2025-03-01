package org.move.lang.core.resolve.ref

import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedElement

/**
 * Used as a resolve result in [org.rust.lang.core.resolve.ref.RsPathReferenceImpl]
 */
data class RsPathResolveResult(
    val element: MvNamedElement,
    val isVisible: Boolean,
): ResolveResult {
    override fun getElement(): PsiElement = element

    override fun isValidResult(): Boolean = true
}
