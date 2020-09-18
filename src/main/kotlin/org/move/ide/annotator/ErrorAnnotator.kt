package org.move.ide.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.move.lang.MoveElementTypes.R_PAREN
import org.move.lang.core.psi.*
import org.move.lang.core.psi.ext.expectedParamsCount
import org.move.lang.core.psi.ext.findFirstChildByType
import org.move.lang.core.psiElement
import org.move.utils.pluralise

class ErrorAnnotator : MoveAnnotator() {
    override fun annotateInternal(element: PsiElement, holder: AnnotationHolder) {
        val visitor = object : MoveVisitor() {
            override fun visitConstDef(o: MoveConstDef) = checkConstDef(holder, o)

            override fun visitFunctionDef(o: MoveFunctionDef) = checkFunctionDef(holder, o)
            override fun visitNativeFunctionDef(o: MoveNativeFunctionDef) = checkNativeFunctionDef(holder, o)

            override fun visitModuleDef(o: MoveModuleDef) = checkModuleDef(holder, o)

            override fun visitStructDef(o: MoveStructDef) = checkStructDef(holder, o)
            override fun visitNativeStructDef(o: MoveNativeStructDef) = checkNativeStructDef(holder, o)
            override fun visitStructFieldDef(o: MoveStructFieldDef) = checkStructFieldDef(holder, o)

            override fun visitCallArguments(o: MoveCallArguments) = checkCallArguments(holder, o)
//            override fun visitAcquiresType(o: MoveAcquiresType) = checkAcquiresType(holder, o)
        }
        element.accept(visitor)
    }

    private fun checkFunctionDef(holder: AnnotationHolder, fn: MoveFunctionDef) {
        checkDuplicates(holder, fn)
        warnOnBuiltInFunctionName(holder, fn)
    }

    private fun checkNativeFunctionDef(holder: AnnotationHolder, nativeFn: MoveNativeFunctionDef) {
        checkDuplicates(holder, nativeFn)
        warnOnBuiltInFunctionName(holder, nativeFn)
    }

    private fun checkModuleDef(holder: AnnotationHolder, mod: MoveModuleDef) {
        checkDuplicates(holder, mod)
    }

    private fun checkStructDef(holder: AnnotationHolder, struct: MoveStructDef) {
        checkDuplicates(holder, struct)
    }

    private fun checkNativeStructDef(holder: AnnotationHolder, nativeStruct: MoveNativeStructDef) {
        checkDuplicates(holder, nativeStruct)
    }

    private fun checkStructFieldDef(holder: AnnotationHolder, structField: MoveStructFieldDef) {
        checkDuplicates(holder, structField)
    }

    private fun checkConstDef(holder: AnnotationHolder, const: MoveConstDef) {
        checkDuplicates(holder, const)
    }
//
//    private fun checkAcquiresType(holder: AnnotationHolder, acquires: MoveAcquiresType) {
//        val types = acquires.qualifiedPathTypeList
//        types.map { it.qualifiedPath.text }
//    }

    private fun checkCallArguments(holder: AnnotationHolder, arguments: MoveCallArguments) {
        val expectedCount = (arguments.parent as? MoveCallExpr)?.expectedParamsCount() ?: return
        val realCount = arguments.exprList.size
        val errorMessage =
            "This function takes $expectedCount ${pluralise(expectedCount, "parameter", "parameters")} " +
                    "but $realCount ${pluralise(realCount, "parameter", "parameters")} " +
                    "${pluralise(realCount, "was", "were")} supplied"
        when {
            realCount < expectedCount -> {
                val target = arguments.findFirstChildByType(R_PAREN) ?: arguments
                val builder = holder.newAnnotation(HighlightSeverity.ERROR, errorMessage)
                builder.range(target)
                builder.create()
            }
            realCount > expectedCount -> {
                arguments.exprList.drop(expectedCount).forEach {
                    val builder = holder.newAnnotation(HighlightSeverity.ERROR, errorMessage)
                    builder.range(it)
                    builder.create()
                }
            }
        }
    }
}

private fun checkDuplicates(
    holder: AnnotationHolder,
    element: MoveNameIdentifierOwner,
    scope: PsiElement = element.parent,
) {
    val duplicateNamedChildren = getDuplicatedNamedChildren(scope)
    if (element.name !in duplicateNamedChildren.map { it.name }) {
        return
    }
    val identifier = element.nameIdentifier ?: element
    val builder =
        holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate definitions with name `${element.name}`")
    builder.range(identifier)
    builder.create()
}

private fun getDuplicatedNamedChildren(owner: PsiElement): Set<MoveNamedElement> {
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

private fun warnOnBuiltInFunctionName(holder: AnnotationHolder, element: MoveNamedElement) {
    val nameElement = element.nameElement ?: return
    val name = element.name ?: return
    if (name in BUILTIN_FUNCTIONS) {
        val builder = holder.newAnnotation(HighlightSeverity.ERROR,
            "Invalid function name: `$name` is a built-in function")
        builder.range(nameElement)
        builder.create()
    }
}
