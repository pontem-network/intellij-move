package org.move.lang.completion.names

import org.move.utils.tests.completion.CompletionTestCase

class StructsCompletionTest: CompletionTestCase() {
    fun `test struct name as type for let binding`() = doSingleCompletion("""
        module 0x1::M {
            struct Frobnicate {}
            fun main() {
                let x: Frob/*caret*/;
            }
        }
    """, """
        module 0x1::M {
            struct Frobnicate {}
            fun main() {
                let x: Frobnicate/*caret*/;
            }
        }
    """)

    fun `test struct name as type for function parameter`() = doSingleCompletion("""
        module 0x1::M {
            struct Frobnicate {}
            fun main(param: Frob/*caret*/) {}
        }
    """, """
        module 0x1::M {
            struct Frobnicate {}
            fun main(param: Frobnicate/*caret*/) {}
        }
    """)

//    fun `test struct spec`() = doSingleCompletion("""
//        module 0x1::M {
//            struct Frobnicate {}
//            spec struct Frob/*caret*/
//        }
//    """, """
//        module 0x1::M {
//            struct Frobnicate {}
//            spec struct Frobnicate /*caret*/
//        }
//    """)

    fun `test type parameters accessible in fields types completion`() = doSingleCompletion("""
        module 0x1::M {
            struct MyStruct<CoinType> { 
                val: Coin/*caret*/ 
            }
        }
    """, """
        module 0x1::M {
            struct MyStruct<CoinType> { 
                val: CoinType/*caret*/ 
            }
        }
    """)

    fun `test struct with type parameters`() = doSingleCompletion("""
        module 0x1::M {
            struct Frobnicate<T> { val: T }
            fun main() {
                let x: Frob/*caret*/;
            }
        }
    """, """
        module 0x1::M {
            struct Frobnicate<T> { val: T }
            fun main() {
                let x: Frobnicate</*caret*/>;
            }
        }
    """)

    fun `test struct with type parameters angle brackets already exist`() = doSingleCompletion("""
        module 0x1::M {
            struct Frobnicate<T> { val: T }
            fun main() {
                let x: Frob/*caret*/<>;
            }
        }
    """, """
        module 0x1::M {
            struct Frobnicate<T> { val: T }
            fun main() {
                let x: Frobnicate</*caret*/>;
            }
        }
    """)

    fun `test struct fields completion`() = doSingleCompletion("""
        module 0x1::M {
            struct T { my_field: u8 }
            fun main() {
                T { my_/*caret*/ }
            }
        }        
    """, """
        module 0x1::M {
            struct T { my_field: u8 }
            fun main() {
                T { my_field/*caret*/ }
            }
        }        
    """)

    fun `test struct fields completion all fields are filled`() = checkNoCompletion("""
        module 0x1::M {
            struct T { my_field: u8 }
            fun main() {
                T {  my_/*caret*/ my_field }
            }
        }        
    """)

    fun `test struct fields completion in presence of shorthand`() = doSingleCompletion("""
        module 0x1::M {
            struct T { my_field: u8 }
            fun main() {
                let my_field = 1;
                T { my_/*caret*/ };
            }
        }
    """, """
        module 0x1::M {
            struct T { my_field: u8 }
            fun main() {
                let my_field = 1;
                T { my_field/*caret*/ };
            }
        }
    """)

    fun `test struct name completion for let pattern`() = doSingleCompletion("""
        module 0x1::M {
            struct Res { my_field: u8 }
            fun main() {
                let R/*caret*/
            }
        }        
    """, """
        module 0x1::M {
            struct Res { my_field: u8 }
            fun main() {
                let Res/*caret*/
            }
        }        
    """)

    fun `test no struct name completion if lower case name`() = checkNoCompletion("""
        module 0x1::M {
            struct Res { my_field: u8 }
            fun main() {
                let r/*caret*/
            }
        }        
    """)

    fun `test no struct name completion if imported from a different module`() = checkNoCompletion("""
        module 0x1::Ss {
            struct Res { my_field: u8 }
        }
        module 0x1::M {
            use 0x1::Ss::Res;
            fun main() {
                let Re/*caret*/
            }
        }        
    """)

    fun `test struct fields completion in struct pattern`() = doSingleCompletion("""
        module 0x1::M {
            struct T { my_field: u8 }
            fun main() {
                let T { my_/*caret*/: field } = call();
            }
        }        
    """, """
        module 0x1::M {
            struct T { my_field: u8 }
            fun main() {
                let T { my_field/*caret*/: field } = call();
            }
        }        
    """)

    fun `test struct fields completion in struct pattern shorthand`() = doSingleCompletion("""
        module 0x1::M {
            struct T { my_field: u8 }
            fun main() {
                let T { my_/*caret*/ } = call();
            }
        }        
    """, """
        module 0x1::M {
            struct T { my_field: u8 }
            fun main() {
                let T { my_field/*caret*/ } = call();
            }
        }        
    """)

    fun `test no completion in struct pattern if field already specified`() = checkNoCompletion("""
        module 0x1::M {
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

    fun `test no completion in struct lit if field already specified`() = checkNoCompletion("""
        module 0x1::M {
            struct T { offered: u8, collateral: u8 }
            fun main() {
                T { 
                    off/*caret*/ 
                    offered: _, 
                    collateral
                };
            }
        }        
    """)

    fun `test module struct completion in type position`() = doSingleCompletion("""
        module 0x1::Transaction {
            struct Type {
                val: u8                   
            }
        }
        module 0x1::M {
            fun main(a: 0x1::Transaction::T/*caret*/) {
            }
        }
    """, """
        module 0x1::Transaction {
            struct Type {
                val: u8                   
            }
        }
        module 0x1::M {
            fun main(a: 0x1::Transaction::Type/*caret*/) {
            }
        }
    """
    )

    fun `test no generics added for acquires`() = doSingleCompletion("""
    module 0x1::M {
        struct Loan<Offered> {}
        fun call() acquires Lo/*caret*/ {}
    }    
    """, """
    module 0x1::M {
        struct Loan<Offered> {}
        fun call() acquires Loan/*caret*/ {}
    }    
    """)

    fun `test add import when completion is selected if not in scope`() = doSingleCompletion("""
    module 0x1::M {
        struct MyStruct {}
    }
    module 0x1::Main {
        fun call(): MySt/*caret*/
    }
    """, """
    module 0x1::M {
        struct MyStruct {}
    }
    module 0x1::Main {
        use 0x1::M::MyStruct;

        fun call(): MyStruct/*caret*/
    }
    """)

    fun `test no import if struct literal is used`() = doSingleCompletion("""
    module 0x1::Main {
        struct UserInfo { name: vector<u8> }
        fun set_name() {
            let a = UserIn/*caret*/
        } 
    }    
    """, """
    module 0x1::Main {
        struct UserInfo { name: vector<u8> }
        fun set_name() {
            let a = UserInfo/*caret*/
        } 
    }    
    """)

    fun `test complete field name in pattern`() = doSingleCompletion("""
    module 0x1::Main {
        struct UserInfo { name: vector<u8> }
        fun set_name() {
            let UserInfo { n/*caret*/ }
        }
    }    
    """, """
    module 0x1::Main {
        struct UserInfo { name: vector<u8> }
        fun set_name() {
            let UserInfo { name/*caret*/ }
        }
    }    
    """)

    fun `test match expr enum variant completion`() = doSingleCompletion("""
        module 0x1::m {
            enum Color { Red, Blue }
            fun main(s: Color) {
                match (s) {
                    Re/*caret*/
                }
            }
        }        
    """, """
        module 0x1::m {
            enum Color { Red, Blue }
            fun main(s: Color) {
                match (s) {
                    Red/*caret*/
                }
            }
        }        
    """)

    fun `test match expr enum item completion`() = doSingleCompletion("""
        module 0x1::m {
            enum Color { Red, Blue }
            fun main(s: Color) {
                match (s) {
                    Col/*caret*/
                }
            }
        }        
    """, """
        module 0x1::m {
            enum Color { Red, Blue }
            fun main(s: Color) {
                match (s) {
                    Color/*caret*/
                }
            }
        }        
    """)

    fun `test fq enum completion`() = doSingleCompletion("""
        module 0x1::m {
            enum Color { Red, Blue }
            fun main() {
                let s: 0x1::m::Col/*caret*/
            }
        }        
    """, """
        module 0x1::m {
            enum Color { Red, Blue }
            fun main() {
                let s: 0x1::m::Color/*caret*/
            }
        }        
    """)
}
