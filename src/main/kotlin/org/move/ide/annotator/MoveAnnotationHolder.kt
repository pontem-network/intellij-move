package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement

class MoveAnnotationHolder(private val holder: AnnotationHolder) {
    fun createErrorAnnotation(element: PsiElement, message: String?) {
        newErrorAnnotation(element, message)?.create()
    }

    fun createWeakWarningAnnotation(element: PsiElement, message: String?) {
        newWeakWarningAnnotation(element, message)?.create()
    }

    fun newErrorAnnotation(
        element: PsiElement,
        message: String?,
    ): AnnotationBuilder? =
        newAnnotation(element, HighlightSeverity.ERROR, message)

    fun newWeakWarningAnnotation(
        element: PsiElement,
        message: String?,
    ): AnnotationBuilder? = newAnnotation(element, HighlightSeverity.WEAK_WARNING, message)

    fun newAnnotation(
        element: PsiElement,
        severity: HighlightSeverity,
        message: String?,
    ): AnnotationBuilder? {
        val builder = if (message == null) {
            holder.newSilentAnnotation(severity)
        } else {
            holder.newAnnotation(severity, message)
        }
        builder.range(element)
        return builder
    }
}

fun pluralise(count: Int, singular: String, plural: String): String =
    if (count == 1) singular else plural
