package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveModulesTest : ResolveTestCase() {
//    fun `test resolve module to imported module`() = checkByCode(
//        """
//        module M {
//            use 0x1::Transaction;
//            fun main() {
//                let a = Transaction::create();
//                      //^
//            }
//        }
//    """
//    )

//    fun `test resolve module to Self`() = checkByCode(
//        """
//        module M {
//            use 0x1::Transaction::{Self};
//                                 //X
//            fun main() {
//                let a = Transaction::create();
//                      //^
//            }
//        }
//    """
//    )

    fun `test resolve module to imported module with alias`() = checkByCode(
        """
        module M {
            use 0x1::Transaction as MyTransaction;
                                  //X
            fun main() {
                let a = MyTransaction::create();
                      //^
            }
        }
    """
    )

    fun `test cannot resolve module if imported one has different address`() = checkByCode(
        """
    address 0x1 {
        module Transaction {}
    }
    
    address 0x2 {
        module M {
            fun main() {
                let a = 0x3::Transaction::create();
                           //^ unresolved
            }
        }
    }
    """
    )

    fun `test resolve module to different one from import`() = checkByCode(
        """
        address 0x1 {
            module A {
                 //X
            }

            module B {
                use 0x1::A;
                       //^
            }
        }
    """
    )

    fun `test no external resolution for alias`() = checkByCode(
        """
        address 0x1 {
            module A {
            }

            module B {
                use 0x1::A as AliasA;
                            //X
                
                fun main() {
                    let a = AliasA::create();
                          //^
                }
            }
        }
    """
    )

    fun `test resolve module to different one in same address block`() = checkByCode(
        """
        address 0x1 {
            module A {
                 //X
            }

            module B {
                use 0x1::A;
                
                fun main() {
                    let a = A::create();
                          //^
                }
            }
        }
    """
    )

    fun `test module in the same address block cannot be resolved without use`() = checkByCode(
        """
        address 0x1 {
            module A {
            }

            module B {
                fun main() {
                    let a = A::create();
                          //^ unresolved
                }
            }
        }
    """
    )

    fun `test resolve to different address block from import`() = checkByCode(
        """
        address 0x2 {
            module A {
                 //X
            }
        }
        
        address 0x1 {
            module B {
                use 0x2::A;
                
                fun main() {
                    let a = A::create();
                          //^
                }
            }
        }
    """
    )

    fun `test resolve to different address block from import in script`() = checkByCode(
        """
        address 0x2 {
            module A {
                 //X
            }
        }
        
        script {
            use 0x2::A;
            
            fun main() {
                let a = A::create();
                      //^
            }
        }
    """
    )

    fun `test resolve to different address block fully qualified`() = checkByCode(
        """
        address 0x2 {
            module A {
                 //X
            }
        }
        
        address 0x1 {
            module B {
                fun main() {
                    let a = 0x2::A::create();
                               //^
                }
            }
        }
    """
    )

    fun `test resolve to different address block import`() = checkByCode(
        """
        address 0x2 {
            module A {
                 //X
            }
        }
        
        address 0x1 {
            module B {
                use 0x2::A;
                
                fun main() {
                    let a = A::create();
                          //^
                }
            }
        }
    """
    )
}