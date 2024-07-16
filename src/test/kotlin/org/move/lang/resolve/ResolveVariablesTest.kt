package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveVariablesTest : ResolveTestCase() {
    fun `test function argument`() = checkByCode(
        """
        script {
            fun main(account: &signer) {
                   //X
                account;
              //^
            }
        }
    """
    )

    fun `test locals`() = checkByCode(
        """
        script {
            fun main() {
                let z = 1;
                  //X
                z;
              //^
            }
        }
    """
    )

    fun `test local variable has priority over function variable`() = checkByCode(
        """
        script {
            fun main(z: u8) {
                let z = z + 1;
                  //X
                z;
              //^  
            }
        }    
    """
    )

    fun `test shadowing of variable with another variable`() = checkByCode(
        """
        script {
            fun main() {
                let z = 1;
                let z = z + 1;
                  //X
                z;
              //^
            }
        }
    """
    )

    fun `test shadowing does not happen until end of statement`() = checkByCode(
        """
        script {
            fun main(z: u8) {
                   //X
                let z = z + 1;
                      //^
            }
        }
    """
    )

    fun `test redefinition in nested block`() = checkByCode(
        """
        script {
            fun main() {
                let a = 1;
                  //X
                {
                    let a = 2;
                };
                a;
              //^  
            }
        }
    """
    )

    fun `test destructuring of struct`() = checkByCode(
        """
        module 0x1::M {
            struct MyStruct {
                val: u8
            }
            
            fun destructure() {
                let MyStruct { val } = get_struct();
                             //X
                val;
              //^  
            }
        }
    """
    )

    fun `test destructuring of struct with variable rename`() = checkByCode(
        """
        module 0x1::M {
            struct MyStruct {
                val: u8
            }
            
            fun destructure() {
                let MyStruct { val: myval } = get_struct();
                                  //X
                myval;
              //^  
            }
        }
    """
    )

    fun `test type params used in as statement`() = checkByCode(
        """
        module 0x1::M {
            fun convert<T>() {
                      //X
                1 as T
                   //^
            }
        }
    """
    )

    fun `test consts`() = checkByCode(
        """
        module 0x1::M {
            const ENOT_GENESIS: u64 = 0;
                //X
            fun main() {
                let a = ENOT_GENESIS;
                      //^
            }
        }
    """
    )

    fun `test tuple destructuring`() = checkByCode(
        """
        module 0x1::M {
            fun main() {
                let (a, b) = call();
                   //X
                a;
              //^  
            }
        }
    """
    )

    fun `test variable defined in nested block`() = checkByCode("""
        module 0x1::M {
            fun main() {
                let a = {
                    let b = 1;
                      //X
                    b + 1
                  //^  
                };
            }
        }        
    """)

    fun `test resolve test attribute to test function parameter`() = checkByCode("""
        module 0x1::M {
            #[test(acc = @0x1)]
                  //^
            fun test_add(acc: signer) {
                        //X
            
            }
        }    
    """)

    fun `test no test attribute resolution if not on function`() = checkByCode("""
        module 0x1::M {
            fun test_add(acc: signer) {
                #[test(acc = @0x1)]
                      //^ unresolved
                use 0x1::M;            
            }
        }    
    """)

    fun `test no attribute resolution if not a test attribute`() = checkByCode("""
        module 0x1::M {
            #[test]
            #[expected_failure(abort_code = 1)]
                                 //^ unresolved
            fun call(abort_code: signer) {
                
            }
        }    
    """)

    fun `test test_only const in test function`() = checkByCode("""
    module 0x1::M {
        #[test_only]
        const TEST_CONST: u64 = 1;
              //X
        #[test]
        fun test_a() {
            TEST_CONST;
                //^
        }
    }    
    """)

    fun `test test_only function in test function`() = checkByCode("""
    module 0x1::M {
        #[test_only]
        fun call() {}
           //X
        
        #[test]
        fun test_a() {
            call();
           //^
        }
    }    
    """)
//
//    fun `test resolve function parameters in specs`() = checkByCode("""
//    module 0x1::main {
//        fun call(a: u8, b: u8) {}
//               //X
//    }
//    spec 0x1::main {
//        spec call(a: u8, b: u8) {}
//                //^
//    }
//    """)

    fun `test resolve const expected failure`() = checkByCode("""
module 0x1::string {
    const ERR_ADMIN: u64 = 1;
          //X
}        
#[test_only]
module 0x1::string_tests {
    use 0x1::string;
    
    #[test]
    #[expected_failure(abort_code = string::ERR_ADMIN)]
                                            //^
    fun test_abort() {
        
    }
}
    """)

    fun `test resolve fq const expected failure`() = checkByCode("""
module 0x1::string {
    const ERR_ADMIN: u64 = 1;
          //X
}        
#[test_only]
module 0x1::string_tests {
    #[test]
    #[expected_failure(abort_code = 0x1::string::ERR_ADMIN)]
                                                //^
    fun test_abort() {
        
    }
}
    """)

    fun `test resolve const item expected failure`() = checkByCode("""
module 0x1::string {
    const ERR_ADMIN: u64 = 1;
          //X
}        
#[test_only]
module 0x1::string_tests {
    use 0x1::string::ERR_ADMIN;
    
    #[test]
    #[expected_failure(abort_code = ERR_ADMIN)]
                                     //^
    fun test_abort() {
        
    }
}
    """)

//    fun `test resolve const item with same name imported expected failure`() = checkByCode("""
//module 0x1::string {
//    const ERR_ADMIN: u64 = 1;
//}
//#[test_only]
//module 0x1::string_tests {
//    use 0x1::string::ERR_ADMIN;
//
//    const ERR_ADMIN: u64 = 1;
//            //X
//
//    #[test]
//    #[expected_failure(abort_code = ERR_ADMIN)]
//                                     //^
//    fun test_abort() {
//
//    }
//}
//    """)

    fun `test resolve const item same module expected failure`() = checkByCode("""
#[test_only]
module 0x1::string_tests {
    const ERR_ADMIN: u64 = 1;
        //X
    
    #[test]
    #[expected_failure(abort_code = ERR_ADMIN)]
                                     //^
    fun test_abort() {
        
    }
}
    """)

    fun `test resolve const import expected failure`() = checkByCode("""
module 0x1::string {
    const ERR_ADMIN: u64 = 1;
          //X
}        
#[test_only]
module 0x1::string_tests {
    use 0x1::string::ERR_ADMIN;
                     //^
}
    """)

    fun `test for loop name resolution`() = checkByCode("""
        module 0x1::m {
            fun main() {
                for (ind in 0..10) {
                    //X
                    ind;
                    //^
                }
            }
        }        
    """)

    fun `test cannot resolve path address`() = checkByCode("""
        module 0x1::m {
            fun main() {
                0x1::;
                //^ unresolved
            }
        }        
    """)

    fun `test resolve attribute location`() = checkByCode("""
        module 0x1::m {
                  //X  
            fun main() {
            }
            #[test(location=0x1::m)]
                               //^
            fun test_main() {
                
            }
        }        
    """)
}
