package org.move.lang.resolve

import org.move.utils.tests.resolve.ResolveTestCase

class ResolveSpecsTest: ResolveTestCase() {
    fun `test resolve in specs does not yet work`() = checkByCode("""
    module 0x1::M {
        fun length(): u8 { 1 }
        spec schema NewAbortsIf {
            length: u64;
            aborts_if length <= 0 with 1;
                    //^ unresolved
        }
    }
    """)
}
