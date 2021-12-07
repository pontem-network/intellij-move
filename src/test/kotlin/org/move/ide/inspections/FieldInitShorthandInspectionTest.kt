package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionsTestCase

class FieldInitShorthandInspectionTest : InspectionsTestCase(FieldInitShorthandInspection::class) {

    fun `test not applicable`() = checkFixIsUnavailable(
        "Use initialization shorthand", """
    module 0x1::M {
        fun m() {
            let _ = S { foo: bar/*caret*/, baz: &baz };
        }
    }    
    """, checkWeakWarn = true
    )

    fun `test fix`() = checkFixByText(
        "Use initialization shorthand", """
    module 0x1::M {
        fun m() {
            let _ = S { <weak_warning descr="Expression can be simplified">foo: foo/*caret*/</weak_warning>, baz: quux };
        }
    }    
    """, """
    module 0x1::M {
        fun m() {
            let _ = S { foo/*caret*/, baz: quux };
        }
    }    
    """
    )
}
