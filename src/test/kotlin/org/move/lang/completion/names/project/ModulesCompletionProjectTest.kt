package org.move.lang.completion.names.project

import com.intellij.codeInsight.lookup.LookupElement
import org.intellij.lang.annotations.Language
import org.move.utils.tests.completion.CompletionProjectTestCase
import org.move.utils.tests.replaceCaretMarker

class ModulesCompletionProjectTest : CompletionProjectTestCase() {

    fun `test complete modules from all the files in imports`() = checkContainsCompletionsExact(
        """
        //- Move.toml
        //- sources/module.move
        module 0x1::M1 {}
        //- sources/main.move
        module 0x1::M2 {}
        script {
            use 0x1::/*caret*/
        }
    """, listOf("M1", "M2")
    )

    fun `test complete modules from all the files in fq path`() = checkContainsCompletionsExact(
        """
        //- Move.toml
        //- sources/module.move
        module 0x1::M1 {}
        //- sources/main.move
        module 0x1::M2 {}
        script {
            fun m() {
                0x1::M/*caret*/
            }
        }
    """, listOf("M1", "M2")
    )

    fun `test autocomplete module name with fq path`() {
        @Language("Move")
        val code = """
        //- Move.toml
        [addresses]
        Std = "0x1"
        //- sources/module.move
        module Std::Module {
            fun call() {}
        }
        //- sources/main.move
        script {
            fun m() {
                Std::Mod/*caret*/
            }
        }
        """.trimIndent()

        @Language("Move")
        val fileCodeAfter = """
        script {
            fun m() {
                Std::Mod/*caret*/
            }
        }
        """.trimIndent()

        val testProject = testProjectFromFileTree(code.trimIndent())
        completionFixture.codeInsightFixture.configureFromFileWithCaret(testProject)

        val lookups = completionFixture.codeInsightFixture.completeBasic()
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

        myFixture.checkResult(replaceCaretMarker(fileCodeAfter))
    }
}
