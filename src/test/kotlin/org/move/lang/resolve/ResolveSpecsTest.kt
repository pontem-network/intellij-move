package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveSpecsTest: ResolveTestCase() {
    fun `test resolve apply schema`() = checkByCode(
        """
        module M {
            spec schema ExactlyOne {}
                      //X
            spec module {
                apply ExactlyOne to *;
                    //^
            }
        }
    """
    )
}