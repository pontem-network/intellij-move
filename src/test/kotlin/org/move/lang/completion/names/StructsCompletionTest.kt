package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class StructsCompletionTest: CompletionTestCase() {
    fun `test struct name as type for let binding`() = doSingleCompletion("""
        module M {
            struct Frobnicate {}
            fun main() {
                let x: Frob/*caret*/;
            }
        }
    """, """
        module M {
            struct Frobnicate {}
            fun main() {
                let x: Frobnicate/*caret*/;
            }
        }
    """)

    fun `test struct name as type for function parameter`() = doSingleCompletion("""
        module M {
            struct Frobnicate {}
            fun main(param: Frob/*caret*/) {}
        }
    """, """
        module M {
            struct Frobnicate {}
            fun main(param: Frobnicate/*caret*/) {}
        }
    """)

//    fun `test struct spec`() = doSingleCompletion("""
//        module M {
//            struct Frobnicate {}
//            spec struct Frob/*caret*/
//        }
//    """, """
//        module M {
//            struct Frobnicate {}
//            spec struct Frobnicate /*caret*/
//        }
//    """)

    fun `test type parameters accessible in fields types completion`() = doSingleCompletion("""
        module M {
            struct MyStruct<CoinType> { 
                val: Coin/*caret*/ 
            }
        }
    """, """
        module M {
            struct MyStruct<CoinType> { 
                val: CoinType/*caret*/ 
            }
        }
    """)

    fun `test struct with type parameters`() = doSingleCompletion("""
        module M {
            struct Frobnicate<T> { val: T }
            fun main() {
                let x: Frob/*caret*/;
            }
        }
    """, """
        module M {
            struct Frobnicate<T> { val: T }
            fun main() {
                let x: Frobnicate</*caret*/>;
            }
        }
    """)

    fun `test struct with type parameters angle brackets already exist`() = doSingleCompletion("""
        module M {
            struct Frobnicate<T> { val: T }
            fun main() {
                let x: Frob/*caret*/<>;
            }
        }
    """, """
        module M {
            struct Frobnicate<T> { val: T }
            fun main() {
                let x: Frobnicate</*caret*/>;
            }
        }
    """)

    fun `test struct fields completion`() = doSingleCompletion("""
        module M {
            struct T { my_field: u8 }
            fun main() {
                T { my_/*caret*/ }
            }
        }        
    """, """
        module M {
            struct T { my_field: u8 }
            fun main() {
                T { my_field/*caret*/ }
            }
        }        
    """)

    fun `test struct fields completion all fields are filled`() = checkNoCompletion("""
        module M {
            struct T { my_field: u8 }
            fun main() {
                T {  my_/*caret*/ my_field }
            }
        }        
    """)

    fun `test struct fields completion in presence of shorthand`() = doSingleCompletion("""
        module M {
            struct T { my_field: u8 }
            fun main() {
                let my_field = 1;
                T { my_/*caret*/ };
            }
        }
    """, """
        module M {
            struct T { my_field: u8 }
            fun main() {
                let my_field = 1;
                T { my_field/*caret*/ };
            }
        }
    """)

    fun `test struct fields completion in struct pattern`() = doSingleCompletion("""
        module M {
            struct T { my_field: u8 }
            fun main() {
                let T { my_/*caret*/ } = call();
            }
        }        
    """, """
        module M {
            struct T { my_field: u8 }
            fun main() {
                let T { my_field/*caret*/ } = call();
            }
        }        
    """)

    fun `test no completion in struct pattern if fields specified`() = checkNoCompletion("""
        module M {
            struct T { offered: u8, collateral: u8 }
            fun main() {
                let T { 
                    off/*caret*/ 
                    offered: _, 
                    collateral
                } = call();
            }
        }        
    """)

    fun `test module struct completion in type position`() = doSingleCompletion("""
        address 0x1 {
        module Transaction {
            struct Type {
                val: u8                   
            }
        }
        }
        module M {
            fun main(a: 0x1::Transaction::/*caret*/) {
            }
        }
    """, """
        address 0x1 {
        module Transaction {
            struct Type {
                val: u8                   
            }
        }
        }
        module M {
            fun main(a: 0x1::Transaction::Type/*caret*/) {
            }
        }
    """
    )

    fun `test no generics added for acquires`() = doSingleCompletion("""
    module M {
        struct Loan<Offered> {}
        fun call() acquires Lo/*caret*/ {}
    }    
    """, """
    module M {
        struct Loan<Offered> {}
        fun call() acquires Loan/*caret*/ {}
    }    
    """)

}
