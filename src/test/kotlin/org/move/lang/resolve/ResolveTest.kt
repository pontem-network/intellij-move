package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveTest: ResolveTestCase() {
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