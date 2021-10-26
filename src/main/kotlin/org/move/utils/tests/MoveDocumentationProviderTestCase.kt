package org.move.utils.tests

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.project.rootManager
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.ide.docs.MoveDocumentationProvider
import org.move.lang.core.psi.MoveElement
import org.move.openapiext.findVirtualFile
import org.move.utils.tests.base.findElementAndOffsetInEditor

abstract class MoveDocumentationProviderHeavyTestCase: MoveHeavyTestBase() {
    protected fun doTestByFileTree(
        @Language("Move") code: String,
        @Language("Html") expected: String?,
        findElement: () -> Pair<PsiElement, Int> = { myFixture.findElementAndOffsetInEditor() },
        block: MoveDocumentationProvider.(PsiElement, PsiElement?) -> String?
    ) {
        val fileTree = fileTreeFromText(code)
        val rootDirectory = myModule.rootManager.contentRoots.first()
        val testProject = fileTree.prepareTestProject(myFixture.project, rootDirectory)

        val fileWithCaret =
            rootDirectory.toNioPath().resolve(testProject.fileWithCaret).findVirtualFile()
                ?: error("No file with //^ caret")
        myFixture.configureFromExistingVirtualFile(fileWithCaret)

//        val (originalElement, offset) = findElement()
        val (originalElement, offset) = myFixture.findElementAndOffsetInEditor<MoveElement>()
        val element = DocumentationManager.getInstance(project)
            .findTargetElement(myFixture.editor, offset, myFixture.file, originalElement)!!

        val actual = MoveDocumentationProvider().block(element, originalElement)?.trim()
        if (expected == null) {
            check(actual == null) { "Expected null, got `$actual`" }
        } else {
            check(actual != null) { "Expected not null result" }
            assertSameLines(actual, expected.trimIndent())
        }
    }
}

abstract class MoveDocumentationProviderTestCase : MoveTestBase() {
    protected fun doTest(
        @Language("Move") code: String,
        @Language("Html") expected: String?,
        findElement: () -> Pair<PsiElement, Int> = { myFixture.findElementAndOffsetInEditor() },
        block: MoveDocumentationProvider.(PsiElement, PsiElement?) -> String?
    ) {
        @Suppress("NAME_SHADOWING")
        doTest(code, expected, findElement, block) { actual, expected ->
            assertSameLines(expected.trimIndent(), actual)
        }
    }

//    protected fun doTest(
//        @Language("Move") code: String,
//        expected: Regex?,
//        findElement: () -> Pair<PsiElement, Int> = { myFixture.findElementAndOffsetInEditor() },
//        block: MoveDocumentationProvider.(PsiElement, PsiElement?) -> String?
//    ) {
//        @Suppress("NAME_SHADOWING")
//        doTest(code, expected, findElement, block) { actual, expected ->
//            assertTrue(actual.matches(expected))
//        }
//    }

    protected fun <T> doTest(
        @Language("Move") code: String,
        expected: T?,
        findElement: () -> Pair<PsiElement, Int> = { myFixture.findElementAndOffsetInEditor() },
        block: MoveDocumentationProvider.(PsiElement, PsiElement?) -> String?,
        check: (String, T) -> Unit
    ) {
        InlineFile(myFixture, code, "main.move")

        val (originalElement, offset) = findElement()
        val element = DocumentationManager.getInstance(project)
            .findTargetElement(myFixture.editor, offset, myFixture.file, originalElement)!!

        val actual = MoveDocumentationProvider().block(element, originalElement)?.trim()
        if (expected == null) {
            check(actual == null) { "Expected null, got `$actual`" }
        } else {
            check(actual != null) { "Expected not null result" }
            check(actual, expected)
        }
    }
}
