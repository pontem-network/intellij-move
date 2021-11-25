package org.move.utils.tests.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.intellij.lang.annotations.Language
import org.move.utils.tests.FileTree
import org.move.utils.tests.MoveProjectTestCase
import org.move.utils.tests.replaceCaretMarker

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
}

abstract class CompletionProjectTestCase : MoveProjectTestCase() {
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
        val testProject = testProjectFromFileTree(code.trimIndent())
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
        val testProject = testProjectFromFileTree(before)
        completionFixture.codeInsightFixture.configureFromFileWithCaret(testProject)

        completionFixture.executeSoloCompletion()
        myFixture.checkResult(replaceCaretMarker(after))
    }

}