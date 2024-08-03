package org.move.lang.resolve

import org.move.ide.inspections.fixes.CompilerV2Feat.INDEXING
import org.move.utils.tests.CompilerV2Features
import org.move.utils.tests.resolve.ResolveTestCase

class ResolveTypesTest : ResolveTestCase() {
    fun `test resolve struct as function param type`() = checkByCode(
        """
        module M {
            struct MyStruct {}
                 //X
            
            fun call(s: MyStruct) {}
                      //^
        }
    """
    )

    fun `test resolve struct as return type`() = checkByCode(
        """
        module M {
            struct MyStruct {}
                 //X
            
            fun call(): MyStruct {}
                      //^
        }
    """
    )

    fun `test resolve struct as acquires type`() = checkByCode(
        """
        module M {
            struct MyStruct {}
                 //X
            
            fun call() acquires MyStruct {}
                              //^
        }
    """
    )

    fun `test resolve struct as struct literal`() = checkByCode(
        """
        module M {
            struct MyStruct {}
                 //X
            
            fun call() {
                let a = MyStruct {};
                      //^
            }
        }
    """
    )

    fun `test resolve struct as struct pattern destructuring`() = checkByCode(
        """
        module M {
            struct MyStruct { val: u8 }
                 //X
            
            fun call() {
                let MyStruct { val } = get_struct();
                  //^
            }
        }
    """
    )

    fun `test resolve struct as type param`() = checkByCode(
        """
        module M {
            resource struct MyStruct {}
                          //X
            
            fun call() {
                let a = move_from<MyStruct>();
                                //^
            }
        }
    """
    )

    fun `test resolve struct type param`() = checkByCode(
        """
        module M {
            struct MyStruct<T> {
                          //X
                val: T
                   //^
            }
        }
    """
    )

    fun `test resolve struct type param inside vector`() = checkByCode(
        """
        module M {
            struct MyStruct<T> {
                          //X
                val: vector<T>
                          //^
            }
        }
    """
    )

    fun `test resolve struct type to struct`() = checkByCode(
        """
        module M {
            struct Native {}
                 //X
            fun main(n: Native): u8 {}
                      //^
        }
    """
    )

    fun `test resolve struct type with generics`() = checkByCode(
        """
        module M {
            struct Native<T> {}
                 //X
            fun main(n: Native<u8>): u8 {}
                      //^
        }
    """
    )

    fun `test pass native struct to native fun`() = checkByCode(
        """
        module M {
            native struct Native<T>;
                        //X
            native fun main(n: Native<u8>): u8;
                             //^
        }
    """
    )

//    fun `test resolve type to import`() = checkByCode(
//        """
//        script {
//            use 0x1::Transaction::Sender;
//                                //X
//
//            fun main(s: Sender) {}
//                      //^
//        }
//    """
//    )

    fun `test resolve type from import`() = checkByCode(
        """
        address 0x1 {
            module Transaction {
                struct Sender {}
                     //X
            }
        }
        script {
            use 0x1::Transaction::Sender;
                                //^
        }
    """
    )

    fun `test resolve type from usage`() = checkByCode(
        """
        address 0x1 {
            module Transaction {
                struct Sender {}
                     //X
            }
        }
        script {
            use 0x1::Transaction::Sender;

            fun main(n: Sender) {}
                      //^
        }
    """
    )

    fun `test resolve type to alias`() = checkByCode(
        """
        module M {
            use 0x1::Transaction::Sender as MySender;
                                          //X
            fun main(n: MySender) {}
                      //^
        }
    """
    )

    fun `test resolve return type to alias`() = checkByCode(
        """
        module M {
            use 0x1::Transaction::Sender as MySender;
                                          //X
            fun main(): MySender {}
                      //^
        }
    """
    )

    fun `test function return type to type param`() = checkByCode(
        """
        module M {
            fun main<Token>()
                   //X
                : Token {}
                //^
        }
    """
    )

    fun `test function return type param to type param`() = checkByCode(
        """
        module M {
            struct Coin<Token> {}
            
            fun main<Token>()
                   //X
                    : Coin<Token> {}
                         //^
        }
    """
    )

    fun `test native function return type param to type param`() = checkByCode(
        """
        module M {
            struct Coin<Token> {}
            
            native fun main<Token>()
                          //X
                    : Coin<Token>;
                         //^
        }
    """
    )

    fun `test struct unresolved in name expr`() = checkByCode("""
        address 0x1 {
            module A {
                struct S {}
            }
            module B {
                use 0x1::A;
                fun call() {
                    A::S
                     //^ unresolved                   
                }
            }
        }
    """)

    fun `test resolve type param in native function in spec`() = checkByCode("""
    module 0x1::M {
        spec module {
            /// Native function which is defined in the prover's prelude.
            native fun serialize<MoveValue>(
                                    //X
                v: &MoveValue
                    //^
            ): vector<u8>;
        }
    }    
    """)

    fun `test resolve struct from use item`() = checkByCode("""
    module 0x1::M {
        struct MyStruct {}
               //X
    }    
    module 0x1::Main {
        use 0x1::M::{Self, MyStruct};
                          //^
    }
    """)

//    fun `test resolve type parameters in specs`() = checkByCode("""
//    module 0x1::main {
//        fun call<T>(a: u8, b: u8) {}
//               //X
//    }
//    spec 0x1::main {
//        spec call<T>(a: u8, b: u8) {}
//                //^
//    }
//    """)

    fun `test resolve type for local import`() = checkByCode("""
module 0x1::table {
    struct Table {}
           //X
}        
module 0x1::main {
    struct S<phantom T> has key {}
    fun main() {
        use 0x1::table::Table;
        
        assert!(exists<S<Table>>(@0x1), 1);
                         //^
    }
}        
    """)

    fun `test resolve type for local import in spec`() = checkByCode("""
module 0x1::table {
    struct Table {}
           //X
}        
module 0x1::main {
    struct S<phantom T> has key {}
    fun main() {}
}   
spec 0x1::main {
    spec main {
        use 0x1::table::Table;
        
        assert!(exists<S<Table>>(@0x1), 1);
                         //^
    }
}
    """)

    fun `test type parameter in return`() = checkByCode("""
module 0x1::m {
    public fun remove<K: copy + drop, V>(
                                    //X
        val: V
    ): V {
     //^
        val
    }
}        
    """)

    @CompilerV2Features(INDEXING)
    fun `test resource index expr`() = checkByCode("""
        module 0x1::m {
            struct S has key {}
                 //X
            fun main() {
                S[@0x1];
              //^   
            }
        }        
    """)

    fun `test module resolution not available on type position`() = checkByCode("""
        module 0x1::Transaction {
            struct Type {
                val: u8                   
            }
        }
        module 0x1::M {
            fun main(a: 0x1::Transaction::Transaction) {
                                           //^ unresolved
            }
        }
    """)

    fun `test resolve enum type`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
               //X
            fun main(one: S::One) {
                        //^
            }
        }        
    """)

    fun `test resolve enum type from module`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            use 0x1::m;
            fun main(one: m::S) {
                           //^
            }
        }        
    """)

    fun `test resolve enum type from module with variant`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            use 0x1::m;
            fun main(one: m::S::One) {
                           //^
            }
        }        
    """)

    fun `test resolve enum type from module with the same name`() = checkByCode("""
        module 0x1::S { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            use 0x1::S;
            fun main(one: S::S::One) {
                           //^
            }
        }        
    """)

    fun `test resolve enum type from fully qualified`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            fun main(one: 0x1::m::S) {
                                //^
            }
        }        
    """)

    fun `test resolve enum type from fully qualified with variant`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            fun main(one: 0x1::m::S::One) {
                                //^
            }
        }        
    """)

    fun `test resolve enum type from item import`() = checkByCode("""
        module 0x1::m { 
            enum S { One, Two }
               //X
        }
        module 0x1::main {
            use 0x1::m::S;
            fun main(one: S::One) {
                        //^
            }
        }        
    """)

    fun `test resolve enum variant`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                   //X
            fun main(one: S::One) {
                            //^
            }
        }        
    """)

    fun `test resolve enum variant from module`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                   //X
        }
        module 0x1::main {
            use 0x1::m;
            fun main(one: m::S::One) {
                               //^
            }
        }        
    """)

    fun `test resolve enum variant from module fully qualified`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                   //X
        }
        module 0x1::main {
            fun main(one: 0x1::m::S::One) {
                                   //^
            }
        }        
    """)
}
