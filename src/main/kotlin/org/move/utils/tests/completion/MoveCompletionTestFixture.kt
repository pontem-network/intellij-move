package org.move.utils.tests.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.intellij.lang.annotations.Language
import org.move.utils.tests.InlineFile
import org.move.utils.tests.hasCaretMarker
import org.move.utils.tests.replaceCaretMarker

class MvCompletionTestFixture(
    val myFixture: CodeInsightTestFixture,
    private val defaultFileName: String = "main.move"
) : BaseFixture() {
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
            "Expected zero completions, but one completion was auto inserted: `${element?.text}`."
        }
        check(lookups.isEmpty()) {
            "Expected zero completions, got ${lookups.map { it.lookupString }}."
        }
    }

    fun checkContainsCompletion(@Language("Move") code: String, variant: String) =
        checkContainsCompletion(code, listOf(variant))

    fun checkContainsCompletion(@Language("Move") code: String, variants: List<String>) {
        prepare(code)
        val lookups = myFixture.completeBasic()

        checkNotNull(lookups) {
            "Expected completions that contain $variants, but no completions found"
        }
        for (variant in variants) {
            if (lookups.all { it.lookupString != variant }) {
                error("Expected completions that contain $variant, but got ${lookups.map { it.lookupString }}")
            }
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
