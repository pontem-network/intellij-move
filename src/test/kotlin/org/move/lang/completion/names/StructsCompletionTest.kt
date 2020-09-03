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

//    fun `test struct with type parameters`() = doSingleCompletion("""
//        module M {
//            struct Frobnicate<T> { val: T }
//            fun main() {
//                let x: Frob/*caret*/;
//            }
//        }
//    """, """
//        module M {
//            struct Frobnicate<T> { val: T }
//            fun main() {
//                let x: Frobnicate</*caret*/>;
//            }
//        }
//    """)

//    fun `test struct with type parameters angle brackets already exist`() = doSingleCompletion("""
//        module M {
//            struct Frobnicate<T> { val: T }
//            fun main() {
//                let x: Frob/*caret*/<>;
//            }
//        }
//    """, """
//        module M {
//            struct Frobnicate<T> { val: T }
//            fun main() {
//                let x: Frobnicate</*caret*/>;
//            }
//        }
//    """)
}