package org.move.ide.inspections.fixes

import org.move.ide.inspections.MvTypeCheckInspection
import org.move.utils.tests.annotation.InspectionTestBase

class IntegerCastFixTest : InspectionTestBase(MvTypeCheckInspection::class) {
    fun `test no cast available on untyped integer`() = checkFixIsUnavailable(
        "Cast to 'u64'", """
        module 0x1::m {
            fun main() {
                let a = 1;
                let b: u64 = a/*caret*/;
            }
        }        
    """
    )

    fun `test no cast available on non-integer type`() = checkFixIsUnavailable(
        "Cast to 'u64'", """
        module 0x1::m {
            fun main() {
                let a = @0x1;
                let b: u64 = <error descr="Incompatible type 'address', expected 'u64'">a/*caret*/</error>;
            }
        }        
    """
    )

    fun `test cast u8 integer variable to u64`() = checkFixByText(
        "Cast to 'u64'", """
        module 0x1::m {
            fun main() {
                let a = 1u8;
                let b: u64 = <error descr="Incompatible type 'u8', expected 'u64'">a/*caret*/</error>;
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                let a = 1u8;
                let b: u64 = (a as u64);
            }
        }        
    """
    )

    fun `test change cast from u64 to u128`() = checkFixByText(
        "Change cast type to 'u128'", """
        module 0x1::m {
            fun main() {
                let a = 1u8;
                let b: u128 = <error descr="Incompatible type 'u64', expected 'u128'">(a as/*caret*/ u64)</error>;
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                let a = 1u8;
                let b: u128 = (a as u128);
            }
        }        
    """
    )

    fun `test remove cast`() = checkFixByText(
        "Remove 'u64' cast", """
        module 0x1::m {
            fun main() {
                let a = 1u8;
                let b: u8 = <error descr="Incompatible type 'u64', expected 'u8'">(a as/*caret*/ u64)</error>;
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                let a = 1u8;
                let b: u8 = a;
            }
        }        
    """
    )
}
