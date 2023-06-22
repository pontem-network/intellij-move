package org.move.ide.annotator

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.ext.startOffset

class MvAnnotationHolder(val holder: AnnotationHolder) {
    fun createErrorAnnotation(element: PsiElement, message: String?) {
        newErrorAnnotation(element, message).create()
    }

    fun createErrorAnnotation(range: TextRange, message: String) {
        val builder =
            holder.newAnnotation(HighlightSeverity.ERROR, message)
        builder.range(range)
        builder.create()
    }

    fun createWeakWarningAnnotation(element: PsiElement, message: String?, vararg fixes: IntentionAction) {
        newWeakWarningAnnotation(element, message, *fixes).create()
    }

    fun newErrorAnnotation(
        element: PsiElement,
        message: String?,
    ): AnnotationBuilder =
        newAnnotation(element, HighlightSeverity.ERROR, message)

    fun newWeakWarningAnnotation(
        element: PsiElement,
        message: String?,
        vararg fixes: IntentionAction
    ): AnnotationBuilder = newAnnotation(element, HighlightSeverity.WEAK_WARNING, message, *fixes)

//    fun newAnnotation(
//        element: PsiElement,
//        rangeInParent: TextRange,
//        severity: HighlightSeverity,
//        message: String?
//    ): AnnotationBuilder {
//        val builder = if (message == null) {
//            holder.newSilentAnnotation(severity)
//        } else {
//            holder.newAnnotation(severity, message)
//        }
//        builder.range(element)
//        return builder
//    }

    fun newAnnotation(
        element: PsiElement,
        severity: HighlightSeverity,
        message: String?,
        vararg fixes: IntentionAction
    ): AnnotationBuilder {
        val builder = if (message == null) {
            holder.newSilentAnnotation(severity)
        } else {
            holder.newAnnotation(severity, message)
        }
        builder.range(element)
        for (fix in fixes) {
            builder.withFix(fix)
        }
        return builder
    }
}

fun pluralise(count: Int, singular: String, plural: String): String =
    if (count == 1) singular else plural
