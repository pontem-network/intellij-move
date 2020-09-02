package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveStructTest: ResolveTestCase() {
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
}