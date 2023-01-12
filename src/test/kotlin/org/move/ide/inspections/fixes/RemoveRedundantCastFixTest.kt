package org.move.ide.inspections.fixes

import org.move.ide.inspections.RedundantTypeCastInspection
import org.move.utils.tests.annotation.InspectionTestBase

class RemoveRedundantCastFixTest: InspectionTestBase(RedundantTypeCastInspection::class) {
    fun `test no cast needed from u64 to u64`() = checkFixByText("Remove redundant cast", """
module 0x1::main {
    fun main() {
        (1u64 <warning descr="No cast needed">as /*caret*/u64</warning>);
    }
}        
    """, """
module 0x1::main {
    fun main() {
        (1u64);
    }
}        
    """)
}
