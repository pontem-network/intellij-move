package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.move.lang.core.psi.MoveFunctionDef
import org.move.lang.core.psi.MoveModuleDef
import org.move.lang.core.psi.MoveNativeFunctionDef
import org.move.lang.core.psi.MoveVisitor
import org.move.lang.core.psi.MoveNameIdentifierOwner
import org.move.lang.core.psi.MoveNamedElement

class ErrorAnnotator : AnnotatorBase() {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val visitor = object : MoveVisitor() {
            override fun visitFunctionDef(o: MoveFunctionDef) = checkFunctionDef(holder, o)
            override fun visitNativeFunctionDef(o: MoveNativeFunctionDef) = checkNativeFunctionDef(holder, o)
            override fun visitModuleDef(o: MoveModuleDef) = checkModuleDef(holder, o)
        }
        element.accept(visitor)
    }

    private fun checkFunctionDef(holder: AnnotationHolder, fn: MoveFunctionDef) {
        checkDuplicates(holder, fn)
    }

    private fun checkNativeFunctionDef(holder: AnnotationHolder, nativeFn: MoveNativeFunctionDef) {
        checkDuplicates(holder, nativeFn)
    }

    private fun checkModuleDef(holder: AnnotationHolder, mod: MoveModuleDef) {
        checkDuplicates(holder, mod)
    }
}

private fun checkDuplicates(
    holder: AnnotationHolder,
    element: MoveNameIdentifierOwner,
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

private fun getDuplicateElements(owner: PsiElement): Set<MoveNamedElement> {
    return owner
        .namedChildren()
        .groupBy { it.name }
        .map { it.value }
        .filter { it.size > 1 }
        .flatten()
        .toSet()
}

private fun PsiElement.namedChildren(): Sequence<MoveNamedElement> {
    return this.children.filterIsInstance<MoveNamedElement>().asSequence()
}
