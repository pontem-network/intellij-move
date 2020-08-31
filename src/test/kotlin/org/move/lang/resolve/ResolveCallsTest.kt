package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveCallsTest : ResolveTestCase() {
    fun `test resolve reference to function`() = checkByCode(
        """
        module M {
            fun call(): u8 {
              //X
                1
            }
            
            fun main() {
                call();
              //^
            }
        }
    """
    )

    fun `test resolve reference to native function`() = checkByCode(
        """
        module M {
            native fun call(): u8;
                     //X
            
            fun main() {
                call();
              //^
            }
        }
    """
    )

}