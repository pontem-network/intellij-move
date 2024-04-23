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

    fun `test chained dot access`() = doSingleCompletion("""
        module 0x1::m {
            struct Pool { field: u8 }
            fun main(pool: &mut Pool) {
                pool./*caret*/
                pool.field
            }
        }        
    """, """
        module 0x1::m {
            struct Pool { field: u8 }
            fun main(pool: &mut Pool) {
                pool.field/*caret*/
                pool.field
            }
        }        
    """)

    fun `test receiver style function completion`() = doSingleCompletion("""
        module 0x1::main {
            struct S { field: u8 }
            fun receiver(self: &S): u8 {}
            fun main(s: S) {
                s.rece/*caret*/
            }
        }        
    """, """
        module 0x1::main {
            struct S { field: u8 }
            fun receiver(self: &S): u8 {}
            fun main(s: S) {
                s.receiver()/*caret*/
            }
        }        
    """)

    fun `test receiver style function completion with assignment`() = doSingleCompletion("""
        module 0x1::main {
            struct S { field: u8 }
            fun receiver(self: &S): u8 {}
            fun main(s: S) {
                let f: u8 = s.rece/*caret*/
            }
        }        
    """, """
        module 0x1::main {
            struct S { field: u8 }
            fun receiver(self: &S): u8 {}
            fun main(s: S) {
                let f: u8 = s.receiver()/*caret*/
            }
        }        
    """)

    fun `test receiver style function completion from another module`() = doSingleCompletion("""
        module 0x1::m {
            struct S { field: u8 }
            public fun receiver(self: &S): u8 {}
        }
        module 0x1::main {
            use 0x1::m::S;
            fun main(s: S) {
                s.rece/*caret*/
            }
        }        
    """, """
        module 0x1::m {
            struct S { field: u8 }
            public fun receiver(self: &S): u8 {}
        }
        module 0x1::main {
            use 0x1::m::S;
            fun main(s: S) {
                s.receiver()/*caret*/
            }
        }        
    """)
}
