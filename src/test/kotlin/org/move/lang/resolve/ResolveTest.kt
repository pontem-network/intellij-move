package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveTest: ResolveTestCase() {
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
}