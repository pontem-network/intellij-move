package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveImportsTest: ResolveTestCase() {
//    fun `test resolve type to imported struct`() = checkByCode(
//        """
//        module M {
//            use 0x1::Transaction::Sender;
//                                //X
//            fun main(n: Sender) {}
//                      //^
//        }
//    """
//    )

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

    fun `test resolve function return type to alias`() = checkByCode(
        """
        module M {
            use 0x1::Transaction::Sender as MySender;
                                          //X
            fun main(): MySender {}
                      //^
        }
    """
    )

//    fun `test resolve function to import`() = checkByCode(
//        """
//        module M {
//            use 0x1::Transaction::create;
//                                //X
//            fun main() {
//                create();
//              //^
//            }
//        }
//    """
//    )

    fun `test resolve function to import alias`() = checkByCode(
        """
        module M {
            use 0x1::Transaction::create as mycreate;
                                          //X
            fun main() {
                mycreate();
              //^  
            }
        }
    """
    )

//    fun `test resolve module to import`() = checkByCode(
//        """
//        module M {
//            use 0x1::Transaction;
//                   //X
//            fun main() {
//                Transaction::create();
//              //^
//            }
//        }
//    """
//    )

    fun `test resolve module to import alias`() = checkByCode(
        """
        module M {
            use 0x1::Transaction as MyTransaction;
                                  //X
            fun main() {
                MyTransaction::create();
              //^  
            }
        }
    """
    )

    fun `test resolve type to alias in script`() = checkByCode("""
        script {
            use 0x1::Transaction::Sender as MySender;
                                          //X
            fun main(): MySender {}
        }             //^ 
    """)

//    fun `test resolve use statement to the module in the same address block`() = checkByCode(
//        """
//        address 0x1 {
//            module A {
//               struct Foo {}
//                    //X
//            }
//
//            module B {
//                use 0x1::A::Foo;
//                          //^
//            }
//        }
//        """
//    )
}