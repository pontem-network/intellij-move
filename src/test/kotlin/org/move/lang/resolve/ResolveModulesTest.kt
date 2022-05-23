package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveModulesTest : ResolveTestCase() {
    fun `test import module itself with Self import`() = checkByCode(
        """
    address 0x1 {
        module Transaction {
             //X
            fun create() {}
        }
        
        module M {
            use 0x1::Transaction::Self;
            fun main() {
                let a = Transaction::create();
                      //^
            }
        }
    }        
    """
    )

    fun `test resolve Self to current module`() = checkByCode(
        """
        module Transaction {
             //X
            fun create() {}
            fun main() {
                let a = Self::create();
                      //^
            }
        }
    """
    )

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

    fun `test resolve to different address block import with address normalization`() = checkByCode(
        """
        address 0x0002 {
            module A {
                 //X
            }
        }
        
        address 0x1 {
            module B {
                use 0x02::A;
                
                fun main() {
                    let a = A::create();
                          //^
                }
            }
        }
    """
    )

    fun `test resolve to inline address module def`() = checkByCode(
        """
        module 0x0002::A {}
                     //X
        
        address 0x1 {
            module B {
                use 0x02::A;
                
                fun main() {
                    let a = A::create();
                          //^
                }
            }
        }
    """
    )

    fun `test resolve module use inline`() = checkByCode("""
    module 0x1::M { fun call() {} }
              //X
    module 0x1::M2 {
        fun m() {
            use 0x1::M;
            M::call();
          //^  
        }
    }    
    """)

    fun `test resolve module as identifier`() = checkByCode("""
module 0x1::Signer {}
            //X
module 0x1::M {
    use 0x1::Signer;
    
    fun call() {
        Signer
          //^  
    }
}        
    """)

    fun `test resolve module from Self`() = checkByCode("""
    module 0x1::M {
              //X
        struct MyStruct {}
    }    
    module 0x1::Main {
        use 0x1::M::{Self, MyStruct};
                    //^
    }
    """)

    fun `test resolve module from Self with alias`() = checkByCode("""
    module 0x1::M {
              //X
        struct MyStruct {}
    }    
    module 0x1::Main {
        use 0x1::M::{Self as MyM, MyStruct};
                    //^
    }
    """)
}
