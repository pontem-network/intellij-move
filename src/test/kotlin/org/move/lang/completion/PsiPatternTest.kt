package org.move.lang.completion

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.MovePattern
import org.move.utils.tests.InlineFile
import org.move.utils.tests.MoveTestCase

class PsiPatternTest : MoveTestCase() {
    fun `test on stmt beginning`() = testPattern("""
        //^
    """, MovePattern.onStatementBeginning)

    fun `test on stmt beginning with words`() = testPattern("""
        word2
            //^
    """, MovePattern.onStatementBeginning("word1", "word2"))

    fun `test on stmt beginning after other statement`() = testPattern("""
        script {
            fun main() {
                let a = 1;
                        //^
            }
        }
    """, MovePattern.onStatementBeginning)

    fun `test on stmt beginning after block`() = testPattern("""
        module Foo {}
                   //^
    """, MovePattern.onStatementBeginning)

    fun `test on stmt beginning ignores comments`() = testPattern("""
        script {} /* three */    /* it's greater than two */
                            //^
    """, MovePattern.onStatementBeginning)

    fun `test on stmt beginning negative in middle of other stmt`() = testPatternNegative("""
        module Abc {}
                //^
    """, MovePattern.onStatementBeginning)

    fun `test on stmt beginning negative when not correct startword`() = testPatternNegative("""
        module 
             //^
    """, MovePattern.onStatementBeginning("script"))

    fun `test code statement`() = testPattern("""
        module M {
            fun main() {
                le
               //^    
            }
        }
    """, MovePattern.codeStatement())

    fun `test code statement nested`() = testPattern("""
        module M {
            fun main() {
                {{{
                    le
                   //^    
                }}}
            }
        }
    """, MovePattern.codeStatement())

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