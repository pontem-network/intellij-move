package org.move.utils.tests.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.prevLeaf
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.intellij.lang.annotations.Language
import org.move.lang.MvElementTypes
import org.move.utils.tests.InlineFile
import org.move.utils.tests.hasCaretMarker
import org.move.utils.tests.replaceCaretMarker

class MvCompletionTestFixture(
    val myFixture: CodeInsightTestFixture,
    private val defaultFileName: String = "main.move"
) : BaseFixture() {

    fun invokeCompletion(@Language("Move") code: String): List<LookupElement> {
        prepare(code)
        val completions = myFixture.completeBasic()
        check(completions != null) { "Only a single completion item" }
        return completions.toList()
    }

    fun doFirstCompletion(@Language("Move") code: String, @Language("Move") after: String) {
        check(hasCaretMarker(after)) { "No /*caret*/ marker in `after`" }
        checkByText(code, after.trimIndent()) {
            val variants = myFixture.completeBasic()
            if (variants != null) {
                myFixture.type('\n')
            }
        }
    }

    fun doSingleCompletion(@Language("Move") code: String, @Language("Move") after: String) {
        check(hasCaretMarker(after)) { "No /*caret*/ marker in `after`" }
        checkByText(code, after.trimIndent()) { executeSoloCompletion() }
    }

    fun checkCompletion(
        lookupString: String,
        @Language("Move") before: String,
        @Language("Move") after: String,
        completionChar: Char,
    ) {
        checkByText(before, after.trimIndent()) {
            val items = myFixture.completeBasic()
                ?: return@checkByText // single completion was inserted
            val lookupItem = items.find { it.lookupString == lookupString } ?: return@checkByText
            myFixture.lookup.currentItem = lookupItem
            myFixture.type(completionChar)
        }
    }

    fun checkNoCompletion(@Language("Move") code: String) {
        prepare(code)
        val lookups = myFixture.completeBasic()
        checkNotNull(lookups) {
            val element = myFixture.file.findElementAt(myFixture.caretOffset - 1)
            // handle assert!()
            var elementText = element?.text
            if (element != null) {
                if (element.prevSibling.elementType == MvElementTypes.EXCL) {
                    // IDENTIFIER + ! + (
                    elementText =
                        element.prevSibling.prevSibling.text + element.prevSibling.text + elementText
                }
            }
            "Expected zero completions, but one completion was auto inserted: `$elementText`."
        }
        check(lookups.isEmpty()) {
            "Expected zero completions, got ${lookups.map { it.lookupString }}."
        }
    }

    fun checkContainsCompletion(@Language("Move") code: String, variant: String) =
        checkContainsCompletion(code, listOf(variant))

    fun checkContainsCompletion(@Language("Move") code: String, expectedLookups: List<String>) {
        prepare(code)
        val lookups = myFixture.completeBasic()
        checkNotNull(lookups) {
            "Only a single completion returned, cannot be used with this test, " +
                    "possibly change the test to doSingleCompletion()"
        }

        val lookupStrings = lookups.map { it.lookupString }
        check(lookupStrings.isNotEmpty()) {
            "Expected completions that contain $expectedLookups, but no completions found"
        }
        for (expectedLookup in expectedLookups) {
            if (lookupStrings.all { it != expectedLookup }) {
                error("Expected completions that contain $expectedLookup, but got $lookupStrings")
            }
        }
//        if (lookups.size > expectedLookups.size) {
//            error("Too many completions. " +
//                          "\n   Expected $expectedLookups, " +
//                          "\n   actual $lookupStrings")
//        }
    }

    fun checkContainsCompletionsExact(@Language("Move") code: String, expectedLookups: List<String>) {
        prepare(code)
        val lookups = myFixture.completeBasic()
        checkNotNull(lookups) {
            "Only a single completion returned, cannot be used with this test, " +
                    "possibly change the test to doSingleCompletion()"
        }

        val lookupStrings = lookups.map { it.lookupString }
        check(lookupStrings.isNotEmpty()) {
            "Expected completions that contain $expectedLookups, but no completions found"
        }
        for (expectedLookup in expectedLookups) {
            if (lookupStrings.all { it != expectedLookup }) {
                error("Expected completions that contain $expectedLookup, but got $lookupStrings")
            }
        }
        if (lookups.size > expectedLookups.size) {
            error("Too many completions. " +
                          "\n   Expected $expectedLookups, " +
                          "\n   actual $lookupStrings")
        }
    }

    fun checkNotContainsCompletion(@Language("Move") code: String, variant: String) {
        prepare(code)
        val lookups = myFixture.completeBasic()
//        checkNotNull(lookups) {
//            "Expected completions that contain $variant, but no completions found"
//        }
        if (lookups.any { it.lookupString == variant }) {
            error("Expected completions that don't contain $variant, but got ${lookups.map { it.lookupString }}")
        }
    }

    fun checkNotContainsCompletion(@Language("Move") code: String, variants: List<String>) {
        prepare(code)
        val lookups = myFixture.completeBasic()
//        checkNotNull(lookups) {
//            "Expected completions that contain $variant, but no completions found"
//        }
        for (variant in variants) {
            if (lookups.any { it.lookupString == variant }) {
                error("Expected completions that don't contain $variant, but got ${lookups.map { it.lookupString }}")
            }
        }
    }

    private fun executeSoloCompletion() {
        val lookups = myFixture.completeBasic()

        if (lookups != null) {
            if (lookups.size == 1) {
                // for cases like `frob/*caret*/nicate()`,
                // completion won't be selected automatically.
                myFixture.type('\n')
                return
            }
            fun LookupElement.debug(): String = "$lookupString ($psiElement)"
            error("Expected a single completion, but got ${lookups.size}\n"
                          + lookups.joinToString("\n") { it.debug() })
        }
    }

    private fun checkByText(code: String, after: String, action: () -> Unit) {
        prepare(code)
        action()
        myFixture.checkResult(replaceCaretMarker(after))
    }

    private fun prepare(code: String) {
        InlineFile(myFixture, code.trimIndent(), defaultFileName).withCaret()
    }
}
