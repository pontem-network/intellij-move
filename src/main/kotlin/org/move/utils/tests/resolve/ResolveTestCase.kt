package org.move.utils.tests.resolve

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement
import org.move.utils.tests.MoveTestCase

abstract class ResolveTestCase : MoveTestCase() {
    protected open fun checkByCode(@Language("Move") code: String) =
        checkByCodeGeneric<MoveNamedElement>(code)

    private inline fun <reified T : NavigatablePsiElement> checkByCodeGeneric(
        @Language("Move") code: String
    ) = checkByCodeGeneric(T::class.java, code)

    private fun <T : NavigatablePsiElement> checkByCodeGeneric(
        targetPsiClass: Class<T>,
        @Language("Move") code: String
    ) {
        InlineFile(code, "main.move")

        val (refElement, data, offset) = findElementWithDataAndOffsetInEditor<MoveReferenceElement>("^")

        if (data == "unresolved") {
            val resolved = refElement.reference?.resolve()
            check(resolved == null) {
                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }

        val resolved = refElement.checkedResolve(offset)

        val target = findElementInEditor(targetPsiClass, "X")
        check(resolved == target) {
            "$refElement `${refElement.text}` should resolve to $target (${target.text}), was $resolved (${resolved.text}) instead"
        }
    }
}

fun PsiElement.findReference(offset: Int): PsiReference? = findReferenceAt(offset - textRange.startOffset)

fun PsiElement.checkedResolve(offset: Int): PsiElement {
    val reference = findReference(offset) ?: error("element doesn't have reference")
    val resolved = reference.resolve() ?: error("Failed to resolve `$text`")

    check(reference.isReferenceTo(resolved)) {
        "Incorrect `isReferenceTo` implementation in `${reference.javaClass.name}`"
    }

    return resolved
}

//private fun checkSearchScope(referenceElement: PsiElement, resolvedTo: PsiElement) {
//    val virtualFile = referenceElement.containingFile.virtualFile ?: return
//    check(resolvedTo.useScope.contains(virtualFile)) {
//        "Incorrect `getUseScope` implementation in `${resolvedTo.javaClass.name}`;" +
//                "also this can means that `pub` visibility is missed somewhere in the test"
//    }
//}
