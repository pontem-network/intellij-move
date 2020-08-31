package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveRefsTest: ResolveTestCase() {
    fun `test function argument`() = checkByCode("""
        script {
            fun main(account: &signer) {
                   //X
                account;
              //^
            }
        }
    """)

    fun `test locals`() = checkByCode("""
        script {
            fun main() {
                let z = 1;
                  //X
                z;
              //^
            }
        }
    """)

    fun `test local variable has priority over function variable`() = checkByCode("""
        script {
            fun main(z: u8) {
                let z = z + 1;
                  //X
                z;
              //^  
            }
        }    
    """)

    fun `test shadowing of variable with another variable`() = checkByCode("""
        script {
            fun main() {
                let z = 1;
                let z = z + 1;
                  //X
                z;
              //^
            }
        }
    """)

    fun `test shadowing does not happen until end of statement`() = checkByCode("""
        script {
            fun main(z: u8) {
                   //X
                let z = z + 1;
                      //^
            }
        }
    """)

    fun `test redefinition in nested block`() = checkByCode("""
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
    """)
}