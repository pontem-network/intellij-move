package org.move.lang.completion.names.project

import com.intellij.codeInsight.lookup.LookupElement
import org.intellij.lang.annotations.Language
import org.move.utils.tests.completion.CompletionProjectTestCase
import org.move.utils.tests.replaceCaretMarker

//class FunctionsCompletionProjectTest : CompletionProjectTestCase() {
//    fun `test autocomplete function with fully qualified path and named address`() {
//        @Language("Move")
//        val code = """
//        //- Move.toml
//        [addresses]
//        Std = "0x1"
//        //- sources/module.move
//        module Std::Module {
//            fun call() {}
//        }
//        //- sources/main.move
//        script {
//            fun m() {
//                Std::Mod/*caret*/
//            }
//        }
//        """.trimIndent()
//
//        @Language("Move")
//        val fileCodeAfter = """
//        script {
//            fun m() {
//                Std::Mod/*caret*/
//            }
//        }
//        """.trimIndent()
//
//        val testProject = testProjectFromFileTree(code.trimIndent())
//        completionFixture.codeInsightFixture.configureFromFileWithCaret(testProject)
//
//        val lookups = completionFixture.codeInsightFixture.completeBasic()
//        if (lookups != null) {
//            if (lookups.size == 1) {
//                // for cases like `frob/*caret*/nicate()`,
//                // completion won't be selected automatically.
//                myFixture.type('\n')
//                return
//            }
//            fun LookupElement.debug(): String = "$lookupString ($psiElement)"
//            error("Expected a single completion, but got ${lookups.size}\n"
//                          + lookups.joinToString("\n") { it.debug() })
//        }
//
//        myFixture.checkResult(replaceCaretMarker(fileCodeAfter))
//    }
//}
