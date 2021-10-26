package org.move.ide.docs

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.move.cli.GlobalScope
import org.move.lang.core.psi.MoveNamedAddress
import org.move.lang.core.types.HasType
import org.move.lang.getCorrespondingMoveToml

class MoveDocumentationProvider: AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element is MoveNamedAddress) {
            val name = element.text
            val moveToml = element.containingFile.getCorrespondingMoveToml() ?: return null
            // TODO: add docs for both scopes
            return moveToml.getAddresses(GlobalScope.MAIN)[name]
        }
        if (element !is HasType) return null
        val type = element.resolvedType(emptyMap()) ?: return null
        return type.typeLabel(element)
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        if (contextElement is MoveNamedAddress) return contextElement
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
    }
}
