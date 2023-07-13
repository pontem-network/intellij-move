package org.move.toml

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.move.ide.annotator.MvAnnotatorBase

class MoveTomlErrorAnnotator: MvAnnotatorBase() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile ?: return
        if (file.name != "Move.toml") {
            return
        }
        println()
    }
}