package org.move.lang.completion

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.move.lang.core.MvPsiPattern
import org.move.utils.tests.InlineFile
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.base.findElementInEditor

class MvPsiPatternTest : MvTestBase() {
    fun `test on stmt beginning`() = testPattern("""
        //^
    """, MvPsiPattern.onStatementBeginning())

    fun `test on stmt beginning with words`() = testPattern("""
        word2
            //^
    """, MvPsiPattern.onStatementBeginning("word1", "word2"))

    fun `test on stmt beginning after other statement`() = testPattern("""
        script {
            fun main() {
                let a = 1;
                        //^
            }
        }
    """, MvPsiPattern.onStatementBeginning())

    fun `test on stmt beginning after block`() = testPattern("""
        module Foo {}
                   //^
    """, MvPsiPattern.onStatementBeginning())

    fun `test on stmt beginning ignores comments`() = testPattern("""
        script {} /* three */    /* it's greater than two */
                            //^
    """, MvPsiPattern.onStatementBeginning())

    fun `test on stmt beginning negative in middle of other stmt`() = testPatternNegative("""
        module Abc {}
                //^
    """, MvPsiPattern.onStatementBeginning())

    fun `test on stmt beginning negative when not correct startword`() = testPatternNegative("""
        module 
             //^
    """, MvPsiPattern.onStatementBeginning("script"))

    fun `test code statement`() = testPattern("""
        module M {
            fun main() {
                le
               //^    
            }
        }
    """, MvPsiPattern.codeStatementPattern())

    fun `test code statement nested`() = testPattern("""
        module M {
            fun main() {
                {{{
                    le
                   //^    
                }}}
            }
        }
    """, MvPsiPattern.codeStatementPattern())

    fun `test no code statement in module`() = testPatternNegative("""
        module 0x1::m {
            le
           //^    
        }
    """, MvPsiPattern.codeStatementPattern())

    fun `test no code statement in struct pat`() = testPatternNegative("""
        module 0x1::m {
            fun main() {
                let S { le } = 1;
                      //^
            }
        }
    """, MvPsiPattern.codeStatementPattern())

    fun `test no code statement in struct lit`() = testPatternNegative("""
        module 0x1::m {
            fun main() {
                let s = S { le };
                           //^
            }
        }
    """, MvPsiPattern.codeStatementPattern())

    fun `test code statement allowed in initializer of struct lit`() = testPattern("""
        module 0x1::m {
            fun main() {
                let s = S { field: le };
                                 //^
            }
        }
    """, MvPsiPattern.codeStatementPattern())

    fun `test borrow type`() = testPattern("""
        script {
            fun main(s: &signer) {
                       //^
            }
        }
    """, MvPsiPattern.pathType())

    fun `test module`() = testPattern("""
        module 0x1::m {
            lelele
            //^
        }        
    """, MvPsiPattern.module())

    private inline fun <reified T : PsiElement> testPattern(
        @Language("Move") code: String,
        pattern: ElementPattern<T>,
    ) {
        InlineFile(myFixture, code, "main.move")
        val element = myFixture.findElementInEditor<T>()
        assertTrue(pattern.accepts(element))
    }

    private fun <T> testPatternNegative(@Language("Move") code: String, pattern: ElementPattern<T>) {
        InlineFile(myFixture, code, "main.move")
        val element = myFixture.findElementInEditor<PsiElement>()
        assertFalse(pattern.accepts(element, null))
    }
}
