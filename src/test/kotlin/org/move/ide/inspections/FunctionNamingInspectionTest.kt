package org.move.ide.inspections

import org.move.ide.inspections.lints.FunctionNamingInspection
import org.move.utils.tests.annotation.InspectionsTestCase

class FunctionNamingInspectionTest: InspectionsTestCase(FunctionNamingInspection::class) {
    fun `test function`() = checkByText("""
        module M {
            fun <error descr="Invalid function name: `assert` is a built-in function">assert</error>() {}
        }
    """)

    fun `test native function`() = checkByText("""
        module M {
            native fun <error descr="Invalid function name: `assert` is a built-in function">assert</error>();
        }
    """)
}