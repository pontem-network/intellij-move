package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class DotAccessCompletionTest: CompletionTestCase() {
    fun `test dot access for struct reference`() = doSingleCompletion("""
        module M {
            struct Frobnicate {
                vec: vector<u8>
            }
            fun main(frob: &Frobnicate) {
                frob.v/*caret*/
            }
        }
    """, """
        module M {
            struct Frobnicate {
                vec: vector<u8>
            }
            fun main(frob: &Frobnicate) {
                frob.vec/*caret*/
            }
        }
    """)

    fun `test dot access for mutable struct reference`() = doSingleCompletion("""
        module M {
            struct Frobnicate {
                vec: vector<u8>
            }
            fun main(frob: &mut Frobnicate) {
                frob.v/*caret*/
            }
        }
    """, """
        module M {
            struct Frobnicate {
                vec: vector<u8>
            }
            fun main(frob: &mut Frobnicate) {
                frob.vec/*caret*/
            }
        }
    """)
}
