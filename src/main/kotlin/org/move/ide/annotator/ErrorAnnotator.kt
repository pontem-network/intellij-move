package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MvFunctionDef
import org.move.lang.core.psi.MvModuleDef
import org.move.lang.core.psi.MvNativeFunctionDef
import org.move.lang.core.psi.MvVisitor
import org.move.lang.core.psi.ext.MvNameIdentifierOwner
import org.move.lang.core.psi.ext.MvNamedElement

class ErrorAnnotator : AnnotatorBase() {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val visitor = object : MvVisitor() {
            override fun visitFunctionDef(o: MvFunctionDef) = checkFunctionDef(holder, o)
            override fun visitNativeFunctionDef(o: MvNativeFunctionDef) = checkNativeFunctionDef(holder, o)
            override fun visitModuleDef(o: MvModuleDef) = checkModuleDef(holder, o)
        }
        element.accept(visitor)
    }

    private fun checkFunctionDef(holder: AnnotationHolder, fn: MvFunctionDef) {
        checkDuplicates(holder, fn)
    }

    private fun checkNativeFunctionDef(holder: AnnotationHolder, nativeFn: MvNativeFunctionDef) {
        checkDuplicates(holder, nativeFn)
    }

    private fun checkModuleDef(holder: AnnotationHolder, mod: MvModuleDef) {
        checkDuplicates(holder, mod)
    }
}

private fun checkDuplicates(
    holder: AnnotationHolder,
    element: MvNameIdentifierOwner,
    scope: PsiElement = element.parent
) {
    val duplicateNamedElements = getDuplicateElements(scope)
    if (element.name !in duplicateNamedElements.map { it.name }) {
        return
    }
    val identifier = element.nameIdentifier ?: element
    val builder =
        holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate definitions with name `${element.name}`")
    builder.range(identifier)
    builder.create()
}

private fun getDuplicateElements(owner: PsiElement): Set<MvNamedElement> {
    return owner
        .namedChildren()
        .groupBy { it.name }
        .map { it.value }
        .filter { it.size > 1 }
        .flatten()
        .toSet()
}

private fun PsiElement.namedChildren(): Sequence<MvNamedElement> {
    return this.children.filterIsInstance<MvNamedElement>().asSequence()
}
