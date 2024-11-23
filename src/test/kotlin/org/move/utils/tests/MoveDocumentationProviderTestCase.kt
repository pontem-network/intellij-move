package org.move.utils.tests

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.platform.backend.documentation.DocumentationData
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.ide.docs.MvDocumentationTarget
import org.move.ide.docs.MvPsiDocumentationTargetProvider
import org.move.lang.core.psi.MvElement
import org.move.stdext.chainIf
import org.move.utils.tests.base.findElementAndOffsetInEditor

abstract class MvDocumentationProviderProjectTestCase : MvProjectTestBase() {
    protected fun doTestByFileTree(
        @Language("Move") builder: TreeBuilder,
        @Language("Html") expected: String?,
    ) {
        testProject(builder)

        val (originalElement, offset) = myFixture.findElementAndOffsetInEditor<MvElement>()
        val element = TargetElementUtil.getInstance().findTargetElement(myFixture.editor, TargetElementUtil.getInstance().getAllAccepted(), offset)!!

        val provider = MvPsiDocumentationTargetProvider()
        val target = provider.documentationTarget(element, originalElement)!!
        val doc = target.computeDocumentation()

        val actual = doc?.content()
        if (expected == null) {
            check(actual == null) { "Expected null, got `$actual`" }
        } else {
            check(actual != null) { "Expected not null result" }
            assertSameLines(expected.trimIndent(), actual)
        }
    }
}

abstract class MvDocumentationProviderTestCase : MvTestBase() {
    protected fun doTest(
        @Language("Move") code: String,
        @Language("Html") expected: String?,
        hideStyles: Boolean = true,
        findElement: () -> Pair<PsiElement, Int> = { myFixture.findElementAndOffsetInEditor() },
    ) {
        @Suppress("NAME_SHADOWING")
        doTest(code, expected, findElement, hideStyles) { actual, expected ->
            assertSameLines(expected.trimIndent(), actual)
        }
    }

//    protected fun doTest(
//        @Language("Move") code: String,
//        expected: Regex?,
//        findElement: () -> Pair<PsiElement, Int> = { myFixture.findElementAndOffsetInEditor() },
//        block: MvDocumentationProvider.(PsiElement, PsiElement?) -> String?
//    ) {
//        @Suppress("NAME_SHADOWING")
//        doTest(code, expected, findElement, block) { actual, expected ->
//            assertTrue(actual.matches(expected))
//        }
//    }

    @Suppress("OverrideOnly")
    protected fun <T> doTest(
        @Language("Move") code: String,
        expected: T?,
        findElement: () -> Pair<PsiElement, Int> = { myFixture.findElementAndOffsetInEditor() },
        hideStyles: Boolean = true,
        check: (String, T) -> Unit
    ) {
        InlineFile(myFixture, code, "main.move")

        val (originalElement, offset) = findElement()
        val element = TargetElementUtil.getInstance().findTargetElement(myFixture.editor, TargetElementUtil.getInstance().getAllAccepted(), offset)!!

        val provider = MvPsiDocumentationTargetProvider()
        val target = provider.documentationTarget(element, originalElement)!!
        val doc = target.computeDocumentation()

        val actual = doc?.content()?.chainIf(hideStyles) { hideSpecificStyles() }
        if (expected == null) {
            check(actual == null) { "Expected null, got `$actual`" }
        } else {
            check(actual != null) { "Expected not null result" }
            check(actual, expected)
        }
    }

    protected fun String.hideSpecificStyles(): String = replace(STYLE_REGEX, """style="..."""")

    companion object {
        private val STYLE_REGEX = Regex("""style=".*?"""")
    }
}

@Suppress("UnstableApiUsage")
fun DocumentationResult.content(): String? = (this as? DocumentationData)?.html?.trim()
