package org.move.lang.completion

import org.move.utils.tests.completion.CompletionTestCase

class BuiltinsCompletionTest: CompletionTestCase() {
    fun `test builtin types present in type positions for function param`() = doSingleCompletion("""
        script { 
            fun main(signer: &si/*caret*/) { 
            }
        }
    """, """
        script { 
            fun main(signer: &signer/*caret*/) { 
            }
        }
    """)

    fun `test builtin types present in type positions for let binding`() = doSingleCompletion("""
        script { 
            fun main() {
             let a: u6/*caret*/
            }
        }
    """, """
        script { 
            fun main() {
             let a: u64/*caret*/
            }
        }
    """)

    fun `test builtin types present in type positions for struct fields`() = doSingleCompletion("""
        module M {
            struct MyStruct { val: u6/*caret*/ }
        }
    """, """
        module M {
            struct MyStruct { val: u64/*caret*/ }
        }
    """)

    fun `test no builtin types in expression position`() = checkNoCompletion("""
        script { 
            fun main() {
                let a = u6/*caret*/
            }
        }
    """)
}