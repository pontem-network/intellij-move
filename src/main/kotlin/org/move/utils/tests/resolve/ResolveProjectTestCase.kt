package org.move.utils.tests.resolve

import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MoveNamedElement
import org.move.lang.core.psi.MoveReferenceElement
import org.move.openapiext.findVirtualFile
import org.move.utils.tests.MoveProjectTestCase
import org.move.utils.tests.base.findElementInEditor
import org.move.utils.tests.base.findElementWithDataAndOffsetInEditor

abstract class ResolveProjectTestCase : MoveProjectTestCase() {
    protected fun checkByFileTree(@Language("Move") code: String) {
        checkByFileTree(
            code,
            MoveReferenceElement::class.java,
            MoveNamedElement::class.java
        )
    }

    protected fun <T : PsiElement, R : PsiElement> checkByFileTree(
        @Language("Move") code: String,
        refClass: Class<R>,
        targetClass: Class<T>
    ) {
        val testProject = testProjectFromFileTree(code)
        myFixture.configureFromFileWithCaret(testProject)
//        val fileWithCaret =
//            testProject.rootDirectory.toNioPath()
//                .resolve(testProject.fileWithCaret).findVirtualFile()
//                ?: error("No file with //^ caret")
//        myFixture.configureFromExistingVirtualFile(fileWithCaret)

        val (refElement, data, offset) =
            myFixture.findElementWithDataAndOffsetInEditor(refClass, "^")
        if (data == "unresolved") {
            val resolved = refElement.reference?.resolve()
            check(resolved == null) {
                "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
            }
            return
        }
        val resolved = refElement.checkedResolve(offset)

        val fileWithNamedElement =
            testProject.rootDirectory.toNioPath()
                .resolve(testProject.fileWithNamedElement).findVirtualFile()
                ?: error("No file with //X caret")
        myFixture.configureFromExistingVirtualFile(fileWithNamedElement)

        val target = myFixture.findElementInEditor(targetClass, "X")
        check(resolved == target) {
            "$refElement `${refElement.text}` should resolve to $target (${target.text}), was $resolved (${resolved.text}) instead"
        }
    }
}
