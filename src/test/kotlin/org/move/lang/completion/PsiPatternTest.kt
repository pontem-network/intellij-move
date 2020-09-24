package org.move.lang.completion

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.MovePsiPatterns
import org.move.utils.tests.InlineFile
import org.move.utils.tests.MoveTestCase

class PsiPatternTest : MoveTestCase() {
    fun `test on stmt beginning`() = testPattern("""
        //^
    """, MovePsiPatterns.onStatementBeginning)

    fun `test on stmt beginning with words`() = testPattern("""
        word2
            //^
    """, MovePsiPatterns.onStatementBeginning("word1", "word2"))

    fun `test on stmt beginning after other statement`() = testPattern("""
        script {
            fun main() {
                let a = 1;
                        //^
            }
        }
    """, MovePsiPatterns.onStatementBeginning)

    fun `test on stmt beginning after block`() = testPattern("""
        module Foo {}
                   //^
    """, MovePsiPatterns.onStatementBeginning)

    fun `test on stmt beginning ignores comments`() = testPattern("""
        script {} /* three */    /* it's greater than two */
                            //^
    """, MovePsiPatterns.onStatementBeginning)

    fun `test on stmt beginning negative in middle of other stmt`() = testPatternNegative("""
        module Abc {}
                //^
    """, MovePsiPatterns.onStatementBeginning)

    fun `test on stmt beginning negative when not correct startword`() = testPatternNegative("""
        module 
             //^
    """, MovePsiPatterns.onStatementBeginning("script"))

    fun `test code statement`() = testPattern("""
        module M {
            fun main() {
                le
               //^    
            }
        }
    """, MovePsiPatterns.codeStatement())

    fun `test code statement nested`() = testPattern("""
        module M {
            fun main() {
                {{{
                    le
                   //^    
                }}}
            }
        }
    """, MovePsiPatterns.codeStatement())

    fun `test borrow type`() = testPattern("""
        script {
            fun main(s: &signer) {
                       //^
            }
        }
    """, MovePsiPatterns.qualPathTypeIdentifier())

    private inline fun <reified T : PsiElement> testPattern(
        @Language("Move") code: String,
        pattern: ElementPattern<T>,
    ) {
        InlineFile(myFixture, code, "main.move")
        val element = findElementInEditor<T>()
        assertTrue(pattern.accepts(element))
    }

    private fun <T> testPatternNegative(@Language("Move") code: String, pattern: ElementPattern<T>) {
        InlineFile(myFixture, code, "main.move")
        val element = findElementInEditor<PsiElement>()
        assertFalse(pattern.accepts(element, null))
    }
}