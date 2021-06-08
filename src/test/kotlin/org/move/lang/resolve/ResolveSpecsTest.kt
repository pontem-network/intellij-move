package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveSpecsTest: ResolveTestCase() {
//    fun `test resolve apply schema`() = checkByCode(
//        """
//        module M {
//            spec schema ExactlyOne {}
//                      //X
//            spec module {
//                apply ExactlyOne to *;
//                    //^
//            }
//        }
//    """
//    )

    fun `test schema type param`() = checkByCode(
        """
        module M {
            spec schema ExactlyOne<C> {
                                 //X
                aborts_if !spec_is_currency<C>();
                                          //^
            }
        }
    """
    )
}
