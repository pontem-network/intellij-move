package org.move.lang.resolve

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

    fun `test resolve struct spec`() = checkByCode(
        """
        module M {
            struct MyStruct {}
                 //X
            
            spec struct MyStruct {
                      //^
                assert true;
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

    fun `test resolve type to import`() = checkByCode(
        """
        script {
            use 0x1::Transaction::Sender;
            
            fun main(s: Sender) {}
                      //^ unresolved
        }
    """
    )

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
}