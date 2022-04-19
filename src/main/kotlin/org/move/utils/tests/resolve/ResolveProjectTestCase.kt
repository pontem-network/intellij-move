package org.move.utils.tests.resolve

import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.psi.MvNamedElement
import org.move.lang.core.resolve.MvReferenceElement
import org.move.openapiext.findVirtualFile
import org.move.utils.tests.FileTreeBuilder
import org.move.utils.tests.MvProjectTestCase
import org.move.utils.tests.TestProject
import org.move.utils.tests.base.findElementInEditor
import org.move.utils.tests.base.findElementWithDataAndOffsetInEditor

abstract class ResolveProjectTestCase : MvProjectTestCase() {
    protected fun checkByFileTree(@Language("Move") code: String) {
        checkByFileTree(
            code,
            MvReferenceElement::class.java,
            MvNamedElement::class.java
        )
    }

    open fun checkByFileTree(fileTree: FileTreeBuilder.() -> Unit) {
        checkByFileTree(
            MvReferenceElement::class.java,
            MvNamedElement::class.java,
            fileTree
        )
    }

    protected fun <T : PsiElement, R : PsiElement> checkByFileTree(
        @Language("Move") code: String,
        refClass: Class<R>,
        targetClass: Class<T>
    ) {
        val testProject = testProjectFromFileTree(code)
        checkByTestProject(testProject, refClass, targetClass)
    }

    protected fun <T : PsiElement, R : PsiElement> checkByFileTree(
        refClass: Class<R>,
        targetClass: Class<T>,
        fileTree: FileTreeBuilder.() -> Unit
    ) {
        val testProject = testProjectFromFileTree(fileTree)
        checkByTestProject(testProject, refClass, targetClass)
    }

    private fun <T : PsiElement, R : PsiElement> checkByTestProject(
        testProject: TestProject,
        refClass: Class<R>,
        targetClass: Class<T>
    ) {
        myFixture.configureFromFileWithCaret(testProject)

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
