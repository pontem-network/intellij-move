package org.move.lang.resolve

import org.move.utils.tests.NamedAddress
import org.move.utils.tests.resolve.ResolveTestCase

class ResolveModulesTest : ResolveTestCase() {
    fun `test use module Self`() = checkByCode(
        """
    module 0x1::transaction {
                  //X
    }
    module 0x1::main {
        use 0x1::transaction::Self;
                             //^
    }
    """
    )

    fun `test use module Self in use group`() = checkByCode(
        """
    module 0x1::transaction {
                  //X
    }
    module 0x1::main {
        use 0x1::transaction::{Self};
                              //^
    }
    """
    )

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

    fun `test import module itself with Self group import`() = checkByCode(
        """
    address 0x1 {
        module Transaction {
             //X
            fun create() {}
        }
        
        module M {
            use 0x1::Transaction::{Self};
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
        module 0x1::transaction {
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
        module 0x1::Transaction {}
        module 0x1::m {
            use 0x1::Transaction as MyTransaction;
                                  //X
            fun main() {
                let a = MyTransaction::create();
                      //^
            }
        }
    """
    )

    fun `test unresolved if alias points to unresolved item`() = checkByCode(
        """
        module 0x1::m {
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
    module 0x1::transaction {}
    module 0x1::m {
        fun main() {
            let a = 0x3::transaction::create();
                         //^ unresolved
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

//    fun `test resolve module as identifier`() = checkByCode("""
//module 0x1::Signer {}
//            //X
//module 0x1::M {
//    use 0x1::Signer;
//
//    fun call() {
//        Signer
//          //^
//    }
//}
//    """)

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

    fun `test friend module resolution for test_only modules`() = checkByCode("""
    module 0x1::M {
        #[test_only]
        friend 0x1::MTest;
                   //^
    }    
    #[test_only]
    module 0x1::MTest {}
               //X
    """)

    fun `test resolve friend module`() = checkByCode("""
    module 0x1::myfriend {}
                 //X
    module 0x1::main {
        friend 0x1::myfriend;
                   //^ 
    }    
    """)

    @NamedAddress("aptos_std", "0x1")
    fun `test resolve friend module with named address`() = checkByCode("""
    module aptos_std::myfriend {}
                      //X
    module aptos_std::main {
        friend aptos_std::myfriend;
                           //^ 
    }    
    """)

    fun `test cannot resolve path inside friend to a function`() = checkByCode("""
    module 0x1::myfriend {
        public fun call() {}
    }
    module 0x1::main {
        use 0x1::myfriend;
        
        friend myfriend::call;
                         //^ unresolved 
    }    
    """)

    fun `test friend no module resolution for test_only modules in non test_only case`() = checkByCode("""
    module 0x1::M {
        friend 0x1::MTest;
                   //^ unresolved
    }    
    #[test_only]
    module 0x1::MTest {}
    """)

    fun `test resolve module import inside code block`() = checkByCode("""
module 0x1::string {
          //X
    public fun utf8() {}
}
module 0x1::main {
    fun main() {
        use 0x1::string;
        string::utf8();
        //^
    }
}        
    """)

    fun `test resolve test failure location to module`() = checkByCode("""
module 0x1::string {}
            //X

#[test_only]
module 0x1::string_tests {
    #[test]
    #[expected_failure(abort_code = 1, location = 0x1::string)]
                                                      //^
    fun test_abort() {
        
    }
}        
    """)

    fun `test unresolved named address`() = checkByCode("""
        module 0x1::m {
            use aptos_framework::account;
                //^ unresolved
        }        
    """)

    fun `test unresolved value address`() = checkByCode("""
        module 0x1::m {
            use 0x1::account;
                //^ unresolved
        }        
    """)

    @NamedAddress("aptos_framework", "0x1")
    fun `test resolve module from import with named address`() = checkByCode("""
        module aptos_framework::m1 {}
                              //X
        module 0x1::m {
            use aptos_framework::m1;
                               //^
        }        
    """)

    @NamedAddress("aptos_framework", "0x1")
    fun `test resolve module from import with value address`() = checkByCode("""
        module aptos_framework::m1 {}
                              //X
        module 0x1::m {
            use 0x1::m1;
                   //^
        }        
    """)

    fun `test cannot resolve module with unknown named address`() = checkByCode("""
        module aptos_framework::m1 {}
        module 0x1::m {
            use aptos_framework::m1;
                               //^ unresolved
        }        
    """)

    fun `test cannot resolve named address in incomplete path use stmt`() = checkByCode("""
        module 0x1::m {
            use aptos_framework
               //^ unresolved
        }        
    """)

    @NamedAddress("std", "0x1")
    fun `test resolve fq module with named address`() = checkByCode("""
        module std::mymodule {
                   //X
            public fun call() {}
        }
        module 0x1::main {
            fun main() {
                std::mymodule::call();
                     //^
            }
        }         
    """)

    @NamedAddress("std", "0x1")
    fun `test resolve fq item with named address`() = checkByCode("""
        module std::mymodule {
            public fun call() {}
                      //X
        }
        module 0x1::main {
            fun main() {
                std::mymodule::call();
                               //^
            }
        }         
    """)

    fun `test resolve module to the current one with fq path`() = checkByCode("""
        module 0x1::m {
                  //X
            struct Option {}
            fun main(): 0x1::m::Option {}
                           //^
        }        
    """)
}
