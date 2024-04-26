package org.move.ide.inspections

import org.intellij.lang.annotations.Language
import org.move.utils.tests.annotation.InspectionTestBase

class FieldInitShorthandInspectionTest: InspectionTestBase(FieldInitShorthandInspection::class) {

    fun `test not applicable`() = doFixIsUnavailableTest(
        "Use initialization shorthand", """
    module 0x1::M {
        fun m() {
            let _ = S { foo: bar/*caret*/, baz: &baz };
        }
    }    
    """
    )

    fun `test fix for struct literal`() = doFixTest(
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

    fun `test fix for struct pattern`() = doFixTest(
        "Use pattern shorthand", """
    module 0x1::M {
        struct S { foo: u8 }
        fun m() {
            let foo = 1;
            let S { <weak_warning descr="Expression can be simplified">foo: foo/*caret*/</weak_warning> } = call();
        }
    }    
    """, """
    module 0x1::M {
        struct S { foo: u8 }
        fun m() {
            let foo = 1;
            let S { foo } = call();
        }
    }    
    """
    )

    fun `test fix for schema literal`() = doFixTest(
        "Use initialization shorthand",
        """
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

    private fun doTest(
        @Language("Move") text: String,
    ) =
        checkByText(text, checkWarn = false, checkWeakWarn = true)

    private fun doFixTest(
        fixName: String,
        @Language("Move") before: String,
        @Language("Move") after: String,

        ) =
        checkFixByText(
            fixName, before, after,
            checkWarn = false, checkWeakWarn = true
        )

    private fun doFixIsUnavailableTest(
        fixName: String,
        @Language("Move") text: String,
    ) =
        checkFixIsUnavailable(
            fixName,
            text,
            checkWarn = false, checkWeakWarn = true
        )
}
