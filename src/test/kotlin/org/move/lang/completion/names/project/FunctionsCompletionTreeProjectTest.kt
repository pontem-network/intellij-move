package org.move.lang.completion.names.project

import org.move.utils.tests.completion.CompletionProjectTestCase

class FunctionsCompletionTreeProjectTest : CompletionProjectTestCase() {
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

    fun `test no candidates for non module items if module fq path`() = checkNoCompletion {
        namedMoveToml("MyPackage")
        sources {
            move("option.move", """
                module 0x1::option {
                    struct Option<Element> has copy, drop, store {
                       vec: vector<Element>
                    }
                    public fun none<Element>(): Option<Element> {
                        Option { vec: vector[] }
                    }
                }
            """)
            move("delegation.move", """
                module 0x1::delegation {
                    public fun none_matched() {}
                }                
            """)
            move("main.move", """
                module 0x1::main {
                    use 0x1::option;
                    fun main() {
                        option::none_ma/*caret*/
                    }
                }                                
            """)
        }
    }
}
