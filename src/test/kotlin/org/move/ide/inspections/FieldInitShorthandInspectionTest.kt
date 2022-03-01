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

    fun `test fix for struct literal`() = checkFixByText(
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

    fun `test fix for struct pattern`() = checkFixByText(
        "Use pattern shorthand", """
    module 0x1::M {
        fun m() {
            let S { <weak_warning descr="Expression can be simplified">foo: foo/*caret*/</weak_warning> } = call();
        }
    }    
    """, """
    module 0x1::M {
        fun m() {
            let S { foo } = call();
        }
    }    
    """
    )

    fun `test fix for schema literal`() = checkFixByText(
        "Use initialization shorthand", """
    module 0x1::M {
        spec module {
            include Schema { <weak_warning descr="Expression can be simplified">foo: foo/*caret*/</weak_warning> };
        }
    }    
    """, """
    module 0x1::M {
        spec module {
            include Schema { foo };
        }
    }    
    """
    )
}
