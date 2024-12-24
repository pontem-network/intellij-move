package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class MvInspectionSuppressorTest: InspectionTestBase(MvNeedlessDerefRefInspection::class) {
    fun `test without suppression`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                <weak_warning>*/*caret*/&1</weak_warning>;
            }
        }        
    """
    )

    fun `test with comment suppression`() = checkWarnings(
        """
        module 0x1::m {
            //noinspection needless_deref_ref
            fun main() {
                */*caret*/&1;
            }
        }        
        //noinspection needless_deref_ref
        module 0x1::m2 {
            fun main() {
                */*caret*/&1;
            }
        }        
    """
    )

    fun `test with attribute suppression`() = checkWarnings(
        """
        module 0x1::m {
            #[lint::skip(needless_deref_ref)]
            fun main() {
                */*caret*/&1;
            }
        }        
        #[lint::skip(needless_deref_ref)]
        module 0x1::m2 {
            fun main() {
                */*caret*/&1;
            }
        }        
    """
    )

    fun `test suppress with attribute fix for function`() = checkFixByText(
        "Suppress with '#[lint::skip(needless_deref_ref)]' for function",
        """
        module 0x1::m {
            fun main() {
                <weak_warning>*/*caret*/&1</weak_warning>;
            }
        }        
    """, """
        module 0x1::m {
            #[lint::skip(needless_deref_ref)]
            fun main() {
                *&1;
            }
        }        
    """,
    )

    fun `test suppress with attribute fix for module`() = checkFixByText(
        "Suppress with '#[lint::skip(needless_deref_ref)]' for module",
        """
        module 0x1::m {
            fun main() {
                <weak_warning>*/*caret*/&1</weak_warning>;
            }
        }        
    """, """
        #[lint::skip(needless_deref_ref)]
        module 0x1::m {
            fun main() {
                *&1;
            }
        }        
    """,
    )
}