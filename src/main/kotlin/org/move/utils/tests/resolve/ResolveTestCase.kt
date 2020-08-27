package org.move.utils.tests.resolve

import org.move.utils.tests.MoveTestCase

class ResolveTestCase : MoveTestCase() {
//    protected open fun checkByCode(@Language("Move") code: String) =
//        checkByCodeGeneric<MoveNamedElement>(code)
//
//    protected inline fun <reified T : NavigatablePsiElement> checkByCodeGeneric(
//        @Language("Move") code: String
//    ) = checkByCodeGeneric(T::class.java, code)
//
//    protected fun <T : NavigatablePsiElement> checkByCodeGeneric(
//        targetPsiClass: Class<T>,
//        @Language("Move") code: String
//    ) {
//        InlineFile(code, "main.move")
//
//        val (refElement, data, offset) = findElementWithDataAndOffsetInEditor<MoveReferenceElementBase>("^")
//
//        if (data == "unresolved") {
//            val resolved = refElement.reference?.resolve()
//            check(resolved == null) {
//                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
//            }
//            return
//        }
//
//        val resolved = refElement.checkedResolve(offset)
//        val target = findElementInEditor(targetPsiClass, "X")
//
//        check(resolved == target) {
//            "$refElement `${refElement.text}` should resolve to $target (${target.text}), was $resolved (${resolved.text}) instead"
//        }
//    }
}
//
//fun PsiElement.findReference(offset: Int): PsiReference? = findReferenceAt(offset - startOffset)
//
//fun PsiElement.checkedResolve(offset: Int): PsiElement {
//    val reference = findReference(offset) ?: error("element doesn't have reference")
//    val resolved = reference.resolve() ?: run {
//        val multiResolve = (reference as? MoveReference)?.multiResolve().orEmpty()
//        check(multiResolve.size != 1)
//        if (multiResolve.isEmpty()) {
//            error("Failed to resolve $text")
//        } else {
//            error("Failed to resolve $text, multiple variants:\n${multiResolve.joinToString()}")
//        }
//    }
//
//    check(reference.isReferenceTo(resolved)) {
//        "Incorrect `isReferenceTo` implementation in `${reference.javaClass.name}`"
//    }
//
//    checkSearchScope(this, resolved)
//
//    return resolved
//}
