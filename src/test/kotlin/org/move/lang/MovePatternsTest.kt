package org.move.lang

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.MovePatterns
import org.move.utils.tests.InlineFile
import org.move.utils.tests.MoveTestCase

class MovePatternsTest : MoveTestCase() {
    fun `test on stmt beginning`() = testPattern("""
        //^
    """, MovePatterns.onStatementBeginning)

    fun `test on stmt beginning with words`() = testPattern("""
        word2
            //^
    """, MovePatterns.onStatementBeginning("word1", "word2"))

    fun `test on stmt beginning after other statement`() = testPattern("""
        script {
            fun main() {
                let a = 1;
                        //^
            }
        }
    """, MovePatterns.onStatementBeginning)

    fun `test on stmt beginning after block`() = testPattern("""
        module Foo {}
                   //^
    """, MovePatterns.onStatementBeginning)

    fun `test on stmt beginning ignores comments`() = testPattern("""
        script {} /* three */    /* it's greater than two */
                            //^
    """, MovePatterns.onStatementBeginning)

    fun `test on stmt beginning negative in middle of other stmt`() = testPatternNegative("""
        module Abc {}
                //^
    """, MovePatterns.onStatementBeginning)

    fun `test on stmt beginning negative when not correct startword`() = testPatternNegative("""
        module 
             //^
    """, MovePatterns.onStatementBeginning("script"))

    private inline fun <reified T : PsiElement> testPattern(
        @Language("Move") code: String,
        pattern: ElementPattern<T>
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