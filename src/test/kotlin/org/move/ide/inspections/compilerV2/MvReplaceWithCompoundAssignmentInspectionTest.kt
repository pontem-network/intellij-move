package org.move.ide.inspections.compilerV2

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveV2
import org.move.utils.tests.annotation.InspectionTestBase

@MoveV2
class MvReplaceWithCompoundAssignmentInspectionTest:
    InspectionTestBase(MvReplaceWithCompoundAssignmentInspection::class) {

    fun `test replace variable assignment with plus`() = doFixTest(
        """
        module 0x1::m {
            fun main() {
                let x = 1;
                <weak_warning descr="Can be replaced with compound assignment">/*caret*/x = x + 1</weak_warning>;
            }
        }
        """, """
        module 0x1::m {
            fun main() {
                let x = 1;
                x += 1;
            }
        }
        """
    )

    fun `test replace variable assignment with left shift`() = doFixTest(
        """
        module 0x1::m {
            fun main() {
                let x = 1;
                <weak_warning descr="Can be replaced with compound assignment">/*caret*/x = x << 1</weak_warning>;
            }
        }
        """, """
        module 0x1::m {
            fun main() {
                let x = 1;
                x <<= 1;
            }
        }
        """
    )

    fun `test replace deref assignment with plus`() = doFixTest(
        """
        module 0x1::m {
            fun main(p: &u8) {
                <weak_warning descr="Can be replaced with compound assignment">/*caret*/*p = *p + 1</weak_warning>;
            }
        }
        """, """
        module 0x1::m {
            fun main(p: &u8) {
                *p += 1;
            }
        }
        """
    )

    private fun doTest(@Language("Move") text: String) =
        checkByText(text, checkWarn = false, checkWeakWarn = true)

    private fun doFixTest(
        @Language("Move") before: String,
        @Language("Move") after: String,
    ) =
        checkFixByText("Replace with compound assignment expr", before, after,
                       checkWarn = false, checkWeakWarn = true)
}