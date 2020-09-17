package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class FunctionsCompletionTest : CompletionTestCase() {
    fun `test function call zero args`() = doSingleCompletion("""
        module M {
            fun frobnicate() {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module M {
            fun frobnicate() {}
            fun main() {
                frobnicate()/*caret*/
            }
        }
    """)

    fun `test function call one arg`() = doSingleCompletion("""
        module M {
            fun frobnicate(a: u8) {}
            fun main() {
                frob/*caret*/
            }
        }
    """, """
        module M {
            fun frobnicate(a: u8) {}
            fun main() {
                frobnicate(/*caret*/)
            }
        }
    """)

    fun `test function call with parens`() = doSingleCompletion("""
        module M {
            fun frobnicate() {}
            fun main() {
                frob/*caret*/()
            }
        }
    """, """
        module M {
            fun frobnicate() {}
            fun main() {
                frobnicate()/*caret*/
            }
        }
    """)

    fun `test function call with parens with arg`() = doSingleCompletion("""
        module M {
            fun frobnicate(a: u8) {}
            fun main() {
                frob/*caret*/()
            }
        }
    """, """
        module M {
            fun frobnicate(a: u8) {}
            fun main() {
                frobnicate(/*caret*/)
            }
        }
    """)

    fun `test complete function name in spec fun without parens`() = doSingleCompletion("""
        module M {
            fun frobnicate(a: u8) {}
            spec fun frob/*caret*/
        }
    """, """
        module M {
            fun frobnicate(a: u8) {}
            spec fun frobnicate /*caret*/
        }
    """)

    fun `test spec fun do not add space if already there`() = doSingleCompletion("""
        module M {
            fun frobnicate(a: u8) {}
            spec fun frob/*caret*/ {}
        }
    """, """
        module M {
            fun frobnicate(a: u8) {}
            spec fun frobnicate/*caret*/ {}
        }
    """)

    fun `test define function accessible before definition`() = doSingleCompletion("""
        module M {
            spec module {
                res/*caret*/;
                
                define reserve_exists(): bool {
                   exists<Reserve>(CoreAddresses::CURRENCY_INFO_ADDRESS())
                }
            }
        }
    """, """
        module M {
            spec module {
                reserve_exists()/*caret*/;
                
                define reserve_exists(): bool {
                   exists<Reserve>(CoreAddresses::CURRENCY_INFO_ADDRESS())
                }
            }
        }
    """)

//    fun `test generic function call with type parameters`() = doSingleCompletion("""
//        module M {
//            fun frobnicate<T>(a: T) {}
//            fun main() {
//                frob/*caret*/
//            }
//        }
//    """, """
//        module M {
//            fun frobnicate<T>(a: T) {}
//            fun main() {
//                frobnicate</*caret*/>()
//            }
//        }
//    """)

    fun `test type parameters accessible in types completion`() = doSingleCompletion("""
        module M {
            fun main<CoinType>() {
                let a: Coi/*caret*/
            }
        }
    """, """
        module M {
            fun main<CoinType>() {
                let a: CoinType/*caret*/
            }
        }
    """)
}