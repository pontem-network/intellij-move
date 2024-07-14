package org.move.lang.core.resolve2.ref

import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import org.move.lang.core.psi.MvElement

/**
 * Used as a resolve result in [org.rust.lang.core.resolve.ref.RsPathReferenceImpl]
 */
data class RsPathResolveResult<T: MvElement>(
    val element: T,
//    val resolvedSubst: Substitution = emptySubstitution,
    val isVisible: Boolean,
//    val namespaces: Set<Namespace> = emptySet(),
): ResolveResult {
    override fun getElement(): PsiElement = element

    override fun isValidResult(): Boolean = true
}
