package org.move.ide.formatter

import org.move.utils.tests.MoveTypingTestCase

class AutoIndentTest : MoveTypingTestCase() {
    fun `test script`() = doTestByText(
        """
        script {/*caret*/}
    """, """
        script {
            /*caret*/
        }
    """
    )

    fun `test module`() = doTestByText(
        """
        module M {/*caret*/}
    """, """
        module M {
            /*caret*/
        }
    """
    )

    fun `test address block no indent`() = doTestByText(
        """
        address 0x0 {/*caret*/}
    """, """
        address 0x0 {
        /*caret*/
        }
    """
    )

    fun `test function`() = doTestByText(
        """
        script {
            fun main() {/*caret*/}
        }
    """, """
        script {
            fun main() {
                /*caret*/
            }
        }
    """
    )

    fun `test second function in module`() = doTestByText(
        """
       module M {
           fun main() {}/*caret*/ 
       }
    """, """
       module M {
           fun main() {}
           /*caret*/
       }
    """
    )

    fun `test struct`() = doTestByText(
        """
       module M {
           struct MyStruct {/*caret*/}
       } 
    """, """
       module M {
           struct MyStruct {
               /*caret*/
           }
       } 
    """
    )

    fun `test resource struct`() = doTestByText(
        """
       module M {
           resource struct MyStruct {/*caret*/}
       } 
    """, """
       module M {
           resource struct MyStruct {
               /*caret*/
           }
       } 
    """
    )

    fun `test function params`() = doTestByText(
        """
       script {
           fun main(a: u8, /*caret*/b: u8) {}
       } 
    """, """
       script {
           fun main(a: u8, 
                    /*caret*/b: u8) {}
       } 
    """
    )

    fun `test complex function declaration`() = doTestByText(
        """
       module M {
            public fun is_currency<CoinType>(): bool {/*caret*/
                exists<CurrencyInfo<CoinType>>(CoreAddresses::CURRENCY_INFO_ADDRESS())
            }
       } 
    """, """
       module M {
            public fun is_currency<CoinType>(): bool {
                /*caret*/
                exists<CurrencyInfo<CoinType>>(CoreAddresses::CURRENCY_INFO_ADDRESS())
            }
       } 
    """
    )

    fun `test spec function`() = doTestByText(
        """
       module M {
           spec fun main {/*caret*/}
       } 
    """, """
       module M {
           spec fun main {
               /*caret*/
           }
       } 
    """
    )

    fun `test spec struct`() = doTestByText(
        """
       module M {
           spec struct MyStruct {/*caret*/}
       } 
    """, """
       module M {
           spec struct MyStruct {
               /*caret*/
           }
       } 
    """
    )

    fun `test spec schema`() = doTestByText(
        """
       module M {
           spec schema MyStruct {/*caret*/}
       } 
    """, """
       module M {
           spec schema MyStruct {
               /*caret*/
           }
       } 
    """
    )

    fun `test spec module`() = doTestByText(
        """
       module M {
           spec module {/*caret*/}
       } 
    """, """
       module M {
           spec module {
               /*caret*/
           }
       } 
    """
    )

    fun `test struct literal`() = doTestByText(
        """
       module M {
           fun main() {
               let a = MyStruct {/*caret*/};      
           }
       } 
    """, """
       module M {
           fun main() {
               let a = MyStruct {
                   /*caret*/
               };      
           }
       } 
    """
    )

    fun `test struct literal in call expr`() = doTestByText(
        """
       module M {
           fun main() {
               call(MyStruct {/*caret*/})      
           }
       } 
    """, """
       module M {
           fun main() {
               call(MyStruct {
                   /*caret*/
               })      
           }
       } 
    """
    )
}