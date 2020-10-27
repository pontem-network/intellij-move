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

    fun `test function acquires on the same line as function`() = doTestByText(
        """
        script {
            fun main() /*caret*/acquires T {}
        } 
    """, """
        script {
            fun main() 
            /*caret*/acquires T {}
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

    fun `test let assignment rhs on the next line`() = doTestByText(
        """
        script {
            fun main() {
                let a = /*caret*/get_record();
            }
        }
    """, """
        script {
            fun main() {
                let a = 
                    /*caret*/get_record();
            }
        }
    """
    )

    fun `test assignment rhs on the next line`() = doTestByText(
        """
        script {
            fun main() {
                a = /*caret*/get_record();
            }
        }
    """, """
        script {
            fun main() {
                a = 
                    /*caret*/get_record();
            }
        }
    """
    )

    fun `test const assignment rhs on the next line`() = doTestByText(
        """
        module M {
            const VAL: u8 = /*caret*/1;
        }
    """, """
        module M {
            const VAL: u8 = 
                /*caret*/1;
        }
    """
    )

    fun `test indent for first element of an assignment`() = doTestByText(
        """
        script {
            fun main() {
                /*caret*/a = get_record();
            }
        }
    """, """
        script {
            fun main() {
                
                /*caret*/a = get_record();
            }
        }
    """
    )

    fun `test block expr inside let`() = doTestByText(
        """
        script {
            fun main() {
                let a = {/*caret*/};
            }
        }
    """, """
        script {
            fun main() {
                let a = {
                    /*caret*/
                };
            }
        }
    """
    )

    fun `test if stmt no indentation`() = doTestByText(
        """
        script {
            fun main() {
                /*caret*/if (true) a
            }
        }
    """, """
        script {
            fun main() {
                
                /*caret*/if (true) a
            }
        }
    """
    )

    fun `test if expr body`() = doTestByText(
        """
        script {
            fun main() {
                if (true) /*caret*/a
            }
        }
    """, """
        script {
            fun main() {
                if (true) 
                    /*caret*/a
            }
        }
    """
    )

    fun `test if else block`() = doTestByText(
        """
        script {
            fun main() {
                if (true) a /*caret*/else b
            }
        }
    """, """
        script {
            fun main() {
                if (true) a 
                /*caret*/else b
            }
        }
    """
    )

    fun `test if else body`() = doTestByText(
        """
        script {
            fun main() {
                if (true) 
                    a 
                else /*caret*/b
            }
        }
    """, """
        script {
            fun main() {
                if (true) 
                    a 
                else 
                    /*caret*/b
            }
        }
    """
    )

    fun `test aborts_if with`() = doTestByText(
        """
        module M {
            spec schema AbortsIf {
                aborts_if true /*caret*/with Errors::NOT_PUBLISHED;
            }
        }
    """, """
        module M {
            spec schema AbortsIf {
                aborts_if true 
                    /*caret*/with Errors::NOT_PUBLISHED;
            }
        }
    """
    )

    fun `test ensures stmt no indentation`() = doTestByText(
        """
        module M {
            spec schema AbortsIf {
                /*caret*/assert a == 1;
            }
        }
    """, """
        module M {
            spec schema AbortsIf {
                
                /*caret*/assert a == 1;
            }
        }
    """
    )

    fun `test ensures indentation`() = doTestByText(
        """
        module M {
            spec schema AbortsIf {
                ensures /*caret*/a == 1;
            }
        }
    """, """
        module M {
            spec schema AbortsIf {
                ensures 
                    /*caret*/a == 1;
            }
        }
    """
    )

    fun `test indent expression operator at the next line`() = doTestByText(
        """
        script {
            fun main() {
                get_record() /*caret*/&& mytransaction;
            }
        }
    """, """
        script {
            fun main() {
                get_record() 
                        /*caret*/&& mytransaction;
            }
        }
    """
    )

    fun `test indent expression operator at the next line in let`() = doTestByText(
        """
        script {
            fun main() {
                let alpha = get_record() /*caret*/&& mytransaction;
            }
        }
    """, """
        script {
            fun main() {
                let alpha = get_record() 
                        /*caret*/&& mytransaction;
            }
        }
    """
    )

    fun `test indent before comment`() = doTestByText(
        """
        script {
            fun main() {/*caret*/
                // hello
            }
        }
    """, """
        script {
            fun main() {
                /*caret*/
                // hello
            }
        }
    """
    )

    fun `test indent after commented use`() = doTestByText(
        """
    address 0x0 {
        module M {
            use 0x0::Account;
            // use 0x0::Signer;/*caret*/
            
            fun main() {}
        }
    }
    """, """
    address 0x0 {
        module M {
            use 0x0::Account;
            // use 0x0::Signer;
            /*caret*/
            
            fun main() {}
        }
    }
    """
    )
}