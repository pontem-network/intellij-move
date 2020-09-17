package org.move.lang.completion

import org.intellij.lang.annotations.Language
import org.move.ide.annotator.BUILTIN_FUNCTIONS
import org.move.utils.tests.completion.CompletionTestCase

class BuiltInsCompletionTest : CompletionTestCase() {
    fun `test autocompletion for built-in functions in expr position`() = doTest("""
        module M {
            fun main() {
                /*caret*/
            }
        }    
    """)

    fun `test no builtins in script`() = checkNoCompletion("""
        script {
            fun main() {
                borrow_glo/*caret*/
            }
        }    
    """)

    fun `test parens added`() = doSingleCompletion("""
        module M {
            fun main() {
                borrow_global_m/*caret*/
            }
        }    
    """, """
        module M {
            fun main() {
                borrow_global_mut(/*caret*/)
            }
        }    
    """)

    fun `test parens existed`() = doSingleCompletion("""
        module M {
            fun main() {
                borrow_global_m/*caret*/()
            }
        }    
    """, """
        module M {
            fun main() {
                borrow_global_mut(/*caret*/)
            }
        }    
    """)

    fun `test no builtins in type position`() = checkNoCompletion("""
        module M {
            fun main(a: borrow/*caret*/) {}
        }    
    """)

    fun `test no builtins in qualified path`() = checkNoCompletion("""
        module M {
            fun main() {
                let a = Libra::borrow/*caret*/
            }
        }    
    """)

    private fun doTest(@Language("Move") text: String) {
        val functionNames = BUILTIN_FUNCTIONS
        for (name in functionNames) {
            checkContainsCompletion(name, text)
        }
    }
}