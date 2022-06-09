package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class PhantomTypeParameterInspectionTest : InspectionTestBase(PhantomTypeParameterInspection::class) {
    fun `test no inspection if type parameter is used`() = checkErrors(
        """
    module 0x1::M {
        struct S<T> {
            val: T
        }
    }    
    """
    )

    fun `test no inspection if type parameter unused but already phantom`() = checkErrors(
        """
    module 0x1::M {
        struct S<phantom T> {
            val: u8
        }
    }    
    """
    )

    fun `test show error with quickfix if not used in any field`() = checkFixByText(
        "Declare phantom", """
    module 0x1::M {
        struct S<<error descr="Unused type parameter. Consider declaring it as phantom">/*caret*/T</error>> { val: u8 }
    }    
    """, """
    module 0x1::M {
        struct S</*caret*/phantom T> { val: u8 }
    }    
    """
    )

    fun `test make phantom with abilities`() = checkFixByText(
        "Declare phantom", """
    module 0x1::M {
        struct S<<error descr="Unused type parameter. Consider declaring it as phantom">/*caret*/T: store</error>> { val: u8 }
    }    
    """, """
    module 0x1::M {
        struct S</*caret*/phantom T: store> { val: u8 }
    }    
    """
    )

    fun `test show error with quickfix if used only in phantom positions`() = checkFixByText(
        "Declare phantom", """
    module 0x1::M {
        struct R<phantom RR> {}
        struct S<<error descr="Unused type parameter. Consider declaring it as phantom">/*caret*/T</error>> {
            val: R<T>
        }
    }
    """, """
    module 0x1::M {
        struct R<phantom RR> {}
        struct S<phantom T> {
            val: R<T>
        }
    }
    """
    )

    fun `test cannot be phantom if used`() = checkFixByText(
        "Remove phantom", """
    module 0x1::M {
        struct S<<error descr="Cannot be phantom">/*caret*/phantom T</error>> {
            value: T
        }
    }    
    """, """
    module 0x1::M {
        struct S<T> {
            value: T
        }
    }    
    """
    )
    fun `test no error if phantom and not used`() = checkByText("""
    module 0x1::M {
        struct S<phantom T> {}
    }    
    """
    )
}
