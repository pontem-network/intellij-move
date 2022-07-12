package org.move.lang.types

import org.move.utils.tests.types.TypificationTestCase

class ExpectedTypeTest : TypificationTestCase() {
    fun `test function parameter primitive type`() = testExpectedTypeExpr(
        """
    module 0x1::Main {
        fun call(a: u8) {}
        fun main() {
            call(my_ref);
                //^ u8
        }
    }    
    """
    )

    fun `test function parameter generic explicit type`() = testExpectedTypeExpr(
        """
    module 0x1::Main {
        fun call<T>(a: T) {}
        fun main() {
            call<u8>(my_ref);
                    //^ u8
        }
    }    
    """
    )

    fun `test let statement initializer no pattern explicit type`() = testExpectedTypeExpr(
        """
    module 0x1::Main {
        fun main() {
            let a: u8 = my_ref;
                       //^ u8
        }
    }    
    """
    )

    fun `test let statement struct pattern`() = testExpectedTypeExpr(
        """
    module 0x1::Main {
        struct S { val: u8 }
        fun main() {
            let S { val } = my_ref;
                            //^ 0x1::Main::S
        }
    }    
    """
    )

    fun `test struct field literal`() = testExpectedTypeExpr(
        """
    module 0x1::Main {
        struct S { val: u8 }
        fun main() {
            S { val: my_ref };
                    //^ u8
        }
    }    
    """
    )

    fun `test no expected type if inside other expr`() = testExpectedTypeExpr(
        """
    module 0x1::Main {
        fun call() {
            let a: u8 = 1 + my_ref;
                           //^ <unknown>
        }
    }    
    """
    )
}
