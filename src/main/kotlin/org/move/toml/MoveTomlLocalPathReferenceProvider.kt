package org.move.toml

import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind

class MoveTomlLocalPathReferenceProvider: PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<FileReference> {
        val kind = (element as? TomlLiteral)?.kind ?: return emptyArray()
        if (kind !is TomlLiteralKind.String) return emptyArray()

        val valueRange = ElementManipulators.getValueTextRange(element)
        return FileReferenceSet(
            valueRange.substring(element.text),
            element,
            valueRange.startOffset,
            this,
            false,
            false
        ).allReferences
    }
}
