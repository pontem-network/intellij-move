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
        module M {
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
        module M {
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
        module M {
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
        module M {
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
        module M {
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
        module M {
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

    fun `test resolve variable in struct field shorthand`() = checkByCode("""
        module M {
            struct S { myfield: u8 }
            
            fun main() {
                let myfield = 1;
                  //X
                S { myfield }
                  //^
            }
        }        
    """)
}