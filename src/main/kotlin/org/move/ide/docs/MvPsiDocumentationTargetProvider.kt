package org.move.ide.docs

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.model.Pointer
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import org.move.lang.core.psi.MvElement
import org.move.lang.core.psi.MvNamedAddress
import org.move.lang.core.psi.MvPatBinding
import org.move.lang.core.psi.MvTypeParameter
import org.move.lang.core.psi.ext.MvDocAndAttributeOwner
import org.move.toml.findInlineDocumentation
import org.toml.lang.psi.TomlKeySegment

class MvPsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        if (element is MvElement) {
            return MvDocumentationTarget(element, originalElement)
        }
        if (element is TomlKeySegment) {
            return MvDocumentationTarget(element, originalElement)
        }
        return null
    }
}

@Suppress("UnstableApiUsage")
class MvDocumentationTarget(val element: PsiElement, private val originalElement: PsiElement?) : DocumentationTarget {
    override fun computePresentation(): TargetPresentation {
        val project = element.project
        val file = element.containingFile?.virtualFile

        return TargetPresentation.builder("")
            .backgroundColor(file?.let { VfsPresentationUtil.getFileBackgroundColor(project, file) })
            .presentation()
    }

    override fun computeDocumentation(): DocumentationResult {
        if (element is MvDocAndAttributeOwner) {
            val content = buildString {
                append(DocumentationMarkup.DEFINITION_START)
                element.signature(this)
                append(DocumentationMarkup.DEFINITION_END)

                append(DocumentationMarkup.CONTENT_START)
                append(element.documentationAsHtml())
                append(DocumentationMarkup.CONTENT_END)
            }

            return DocumentationResult.documentation(content)
        }

        val content = buildString {
            append(DocumentationMarkup.DEFINITION_START)
            when (element) {
                is MvTypeParameter -> generateTypeParameter(element)
                is MvPatBinding    -> generatePatBinding(element)
                is MvNamedAddress  -> generateNamedAddress(element)
                is TomlKeySegment  -> {
                    generateTomlKeySegment(element)
                }
            }
            append(DocumentationMarkup.DEFINITION_END)

            append(DocumentationMarkup.CONTENT_START)
            if (element is TomlKeySegment) {
                append(element.findInlineDocumentation())
            }
            append(DocumentationMarkup.CONTENT_END)
        }
        return DocumentationResult.documentation(content)
    }

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val originalElementPtr = originalElement?.createSmartPointer()
        return Pointer {
            val element = elementPtr.dereference() ?: return@Pointer null
            MvDocumentationTarget(element, originalElementPtr?.dereference())
        }
    }
}
