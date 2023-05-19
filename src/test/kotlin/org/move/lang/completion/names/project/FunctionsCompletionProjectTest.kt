package org.move.lang.completion.names.project

import org.move.utils.tests.completion.CompletionProjectTestCase

class FunctionsCompletionProjectTest : CompletionProjectTestCase() {
    fun `test public method is encountered in completion only once`() = doSingleCompletion(
        {
            namedMoveToml("MyPackage")
            tests {
                move(
                    "m.move", """
                    module 0x1::m {
                        public fun add_liquidity() {}
                        fun main() {
                            add_li/*caret*/
                        }
                    }
                """
                )
            }
        }, """
        module 0x1::m {
            public fun add_liquidity() {}
            fun main() {
                add_liquidity()/*caret*/
            }
        }
    """
    )

    fun `test helper test method is encountered in completion only once`() = doSingleCompletion(
        {
            namedMoveToml("MyPackage")
            tests {
                move(
                    "m_tests.move", """
                    #[test_only]
                    module 0x1::m_tests {
                        public fun add_liquidity() {}
                        #[test]
                        fun test_main() {
                            add_li/*caret*/
                        }
                    }
                """
                )
            }
        }, """
        #[test_only]
        module 0x1::m_tests {
            public fun add_liquidity() {}
            #[test]
            fun test_main() {
                add_liquidity()/*caret*/
            }
        }
    """
    )
}
