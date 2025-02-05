package org.move.lang.resolve

import org.move.utils.tests.NamedAddress
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

    @NamedAddress("aptos_std", "0x1")
    fun `test resolve attribute location for named address`() = checkByCode("""
        module aptos_std::m {
                  //X  
            fun main() {
            }
            #[test(location=aptos_std::m)]
                                     //^
            fun test_main() {
                
            }
        }        
    """)

    fun `test resolve variable in match expr`() = checkByCode("""
        module 0x1::m {
            fun main() {
                let m = 1;
                  //X
                match (m) {
                     //^
                }
            }
        }        
    """)

    fun `test resolve function with match name`() = checkByCode("""
        module 0x1::m {
            fun match() {}
              //X
            fun main() {
                match();
                  //^
            }
        }
    """)


    fun `test resolve type in match arm 1`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
               //X
            fun main() {
                let m = 1;
                match (m) {
                    S::One => true
                  //^  
                    S::Two => false
                }
            }
        }        
    """)

    fun `test resolve type in match arm 2`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                    //X
            fun main() {
                let m = 1;
                match (m) {
                    S::One => true
                      //^
                    S::Two => false
                }
            }
        }        
    """)

    fun `test resolve type in match arm 3`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
                         //X
            fun main() {
                let m = 1;
                match (m) {
                    S::One => true
                    S::Two => false
                      //^
                }
            }
        }        
    """)

    fun `test resolve item in match arm body 1`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
            fun main() {
                let m = 1;
                  //X
                match (m) {
                    S::One => m
                            //^
                }
            }
        }        
    """)

    fun `test resolve item in match arm body 2`() = checkByCode("""
        module 0x1::m {
            enum S { One, Two }
            fun main(s: S) {
                   //X
                let m = 1;
                match (m) {
                    S::One => s
                            //^
                }
            }
        }        
    """)

    fun `test enum variant with fields`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                    //X
            fun main() {
                let m = 1;
                match (m) {
                    S::One { field: f } => f
                      //^
                }
            }
        }        
    """)

    fun `test resolve fields for enum variant in match arm`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                           //X
            fun main() {
                let m = 1;
                match (m) {
                    S::One { field: f } => f
                            //^
                }
            }
        }        
    """)

    fun `test resolve shortcut field for enum variant in match arm`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                           //X
            fun main() {
                let m = 1;
                match (m) {
                    S::One { field } => field
                            //^
                }
            }
        }        
    """)

    fun `test resolve binding for field reassignment for enum variant`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
            fun main() {
                let m = 1;
                match (m) {
                    S::One { field: myfield }
                                    //X
                        => myfield
                            //^
                }
            }
        }        
    """)

    fun `test resolve binding for shortcut field for enum variant`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
            fun main() {
                let m = 1;
                match (m) {
                    S::One { field }
                            //X
                        => field
                            //^
                }
            }
        }        
    """)

    fun `test resolve field for struct pat in enum`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                            //X
            fun main(s: S::One) {
                let S::One { field } = s;
                            //^
            }
        }        
    """)

    fun `test resolve field reassignment for struct pat in enum`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                            //X
            fun main(s: S::One) {
                let S::One { field: f } = s;
                            //^
            }
        }        
    """)

    fun `test resolve field reassignment for struct pat in enum binding`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
            fun main(s: S::One) {
                let S::One { field: f } = s;
                                  //X
                f;
              //^  
            }
        }        
    """)

    fun `test resolve enum variant for struct lit`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                           //X
            fun main(s: S::One) {
                let f = 1;
                let s = S::One { field: f };
                                 //^
            }
        }        
    """)

    fun `test resolve enum variant for struct pat`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
               //X
            fun main(s: S::One) {
                let S::One { field } = s;
                  //^
            }
        }        
    """)

    fun `test resolve enum variant for struct pat 2`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                   //X
            fun main(s: S::One) {
                let S::One { field } = s;
                      //^
            }
        }        
    """)

    fun `test resolve field reassignment for struct lit enum variant`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
                           //X
            fun main(s: S::One) {
                let f = 1;
                let s = S::One { field: f };
                                 //^
            }
        }        
    """)

    fun `test resolve field reassignment for struct lit enum variant binding`() = checkByCode("""
        module 0x1::m {
            enum S { One { field: u8 }, Two }
            fun main(s: S::One) {
                let f = 1;
                  //X
                let s = S::One { field: f };
                                      //^
            }
        }        
    """)

    fun `test shadow global spec variable with local one`() = checkByCode("""
        module 0x1::m {
            spec module {
                global supply<CoinType>: num;
            }
            fun main() {
                let supply = 1;
                    //X
                spec {
                    supply;
                    //^ 
                }
            }
        }        
    """)

    fun `test outer block variable with inner block variable`() = checkByCode("""
        module 0x1::m {
            fun main() {
                let supply = 1;
                spec {
                    let supply = 2;
                        //X
                    supply;
                    //^ 
                }
            }
        }        
    """)

    fun `test unknown struct literal variable is resolvable with shorthand`() = checkByCode("""
        module 0x1::m {
            fun main() {
                let myfield = 1;
                     //X
                Unknown { myfield };
                           //^
            }
        }        
    """)

    fun `test unknown struct literal variable is resolvable with full field`() = checkByCode("""
        module 0x1::m {
            fun main() {
                let myfield = 1;
                     //X
                Unknown { field: myfield };
                                //^
            }
        }        
    """)

    @NamedAddress("std", "0x1")
    fun `test function signer with the address name with existing named address`() = checkByCode("""
        module 0x1::m {
            #[test(std = @0x1)]
            fun test_address(std: &signer) {
                            //X
                std;
               //^ 
            }
        }        
    """)

    fun `test resolve tuple struct pattern`() = checkByCode("""
        module 0x1::m {
            struct S(u8, u8);
                 //X
            fun main(s: S) {
                let S ( field1, field2 ) = s;
                  //^
            }
        }        
    """)

    fun `test resolve variables in tuple struct pattern`() = checkByCode("""
        module 0x1::m {
            struct S(u8, u8);
            fun main(s: S) {
                let S ( field1, field2 ) = s;
                          //X
                field1;
                //^
            }
        }        
    """)

    fun `test pattern with rest`() = checkByCode("""
        module 0x1::m {
            struct S { f1: u8, f2: u8 }
                     //X
            fun main(s: S) {
                let S { f1, .. } = s;
                       //^
            }
        }        
    """)

    fun `test compound assigment lhs binding`() = checkByCode("""
        module 0x1::m {
            fun main() {
                let x = 1;
                  //X
                x += 1;
              //^  
            }
        }        
    """)

    fun `test compound assigment rhs binding`() = checkByCode("""
        module 0x1::m {
            fun main() {
                let x = 1;
                let y = 2;
                  //X
                x += y;
                   //^
            }
        }        
    """)

    fun `test no attr item signer reference for not direct children of test`() = checkByCode("""
        module 0x1::m {
            #[test(unknown_attr(my_signer = @0x1))]
                                 //^ unresolved
            fun test_main(my_signer: signer) {
            }
        }        
    """)
}
