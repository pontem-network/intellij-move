package org.move.ide.docs

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import org.move.lang.core.types.HasType

class MoveDocumentationProvider: AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element !is HasType) return null
        val type = element.resolvedType(emptyMap()) ?: return null
        return type.typeLabel(element)
    }
}
