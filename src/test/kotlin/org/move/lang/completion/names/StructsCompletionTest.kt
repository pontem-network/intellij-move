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

    fun `test struct spec`() = doSingleCompletion("""
        module M {
            struct Frobnicate {}
            spec struct Frob/*caret*/
        }
    """, """
        module M {
            struct Frobnicate {}
            spec struct Frobnicate /*caret*/
        }
    """)

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
}