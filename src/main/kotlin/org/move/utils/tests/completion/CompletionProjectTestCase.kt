package org.move.utils.tests.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.intellij.lang.annotations.Language
import org.move.utils.tests.*

class CompletionTestProjectFixture(
    val codeInsightFixture: CodeInsightTestFixture
) : BaseFixture() {
    //    protected fun doSingleCompletion(
//        @Language("Move") before: String, @Language("Move") after: String
//    ) {
//    }
    fun executeSoloCompletion() {
        val lookups = codeInsightFixture.completeBasic()

        if (lookups != null) {
            if (lookups.size == 1) {
                // for cases like `frob/*caret*/nicate()`,
                // completion won't be selected automatically.
                codeInsightFixture.type('\n')
                return
            }
            fun LookupElement.debug(): String = "$lookupString ($psiElement)"
            error("Expected a single completion, but got ${lookups.size}\n"
                          + lookups.joinToString("\n") { it.debug() })
        }
    }

    fun checkNoCompletion() {
        val lookups = codeInsightFixture.completeBasic()
        checkNotNull(lookups) {
            val element = codeInsightFixture.file.findElementAt(codeInsightFixture.caretOffset - 1)
            "Expected zero completions, but one completion was auto inserted: `${element?.text}`."
        }
        check(lookups.isEmpty()) {
            "Expected zero completions, got ${lookups.map { it.lookupString }}."
        }
    }
}

abstract class CompletionProjectTestCase : MvProjectTestBase() {
    lateinit var completionFixture: CompletionTestProjectFixture

    override fun setUp() {
        super.setUp()
        completionFixture = CompletionTestProjectFixture(myFixture)
        completionFixture.setUp()
    }

    override fun tearDown() {
        completionFixture.tearDown()
        super.tearDown()
    }

    protected fun checkContainsCompletionsExact(
        @Language("Move") code: String, expected: List<String>
    ) {
        val testProject = testProject(code.trimIndent())
        checkContainsCompletionsExact(testProject, expected)
    }

    protected fun checkContainsCompletionsExact(
        expected: List<String>, builder: FileTreeBuilder.() -> Unit,
    ) {
        val testProject = testProject(builder)
        checkContainsCompletionsExact(testProject, expected)
    }

    protected fun checkContainsCompletionsExact(
        builder: FileTreeBuilder.() -> Unit, expected: List<String>
    ) {
        val testProject = testProject(builder)
        checkContainsCompletionsExact(testProject, expected)
    }

    private fun checkContainsCompletionsExact(testProject: TestProject, expected: List<String>) {
        completionFixture.codeInsightFixture.configureFromFileWithCaret(testProject)

        val lookups =
            completionFixture.codeInsightFixture.completeBasic()
                .map { it.lookupString }
                .sorted()
        val completions = expected.sorted()
        check(completions == lookups) {
            "Expected completions are: $completions, actual lookups are: $lookups"
        }
    }

    protected fun doSingleCompletion(
        @Language("Move") before: String, @Language("Move") after: String
    ) {
        testProject(before)

        completionFixture.executeSoloCompletion()
        myFixture.checkResult(replaceCaretMarker(after))
    }

    protected fun doSingleCompletion(
        before: FileTreeBuilder.() -> Unit, @Language("Move") after: String
    ) {
        testProject(before)
//        completionFixture.codeInsightFixture.configureFromFileWithCaret(testProject)

        completionFixture.executeSoloCompletion()
        myFixture.checkResult(replaceCaretMarker(after.trimIndent()))
    }

    protected fun checkNoCompletion(builder: FileTreeBuilder.() -> Unit) {
        testProject(builder)
//        completionFixture.codeInsightFixture.configureFromFileWithCaret(testProject)
        completionFixture.checkNoCompletion()
    }

}
