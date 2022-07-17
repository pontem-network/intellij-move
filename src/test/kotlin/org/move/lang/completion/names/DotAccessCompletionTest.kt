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

    fun `test borrow global dot field in test_only module`() = checkContainsCompletion(
        listOf("mint_cap", "burn_cap"),
        """
#[test_only]            
module 0x1::M {
    struct Caps has key { mint_cap: u8, burn_cap: u8 }
    fun main() {
        borrow_global<Caps>(@0x1)./*caret*/
    }
}            
        """
    )
}
