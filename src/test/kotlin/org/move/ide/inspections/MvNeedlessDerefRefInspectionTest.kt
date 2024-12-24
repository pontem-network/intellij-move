package org.move.ide.inspections

import org.intellij.lang.annotations.Language
import org.move.utils.tests.annotation.InspectionTestBase

class MvNeedlessDerefRefInspectionTest: InspectionTestBase(MvNeedlessDerefRefInspection::class) {
    fun `test no error`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                &1;
                *1;
                **1;
                &&1;
            }
        }        
    """
    )

    fun `test no error for deref and then borrow as it can be a copy op`() = checkWarnings(
        """
        module 0x1::m {
            fun main() {
                &*1;
            }
        }        
    """
    )

    fun `test error for borrow and then deref`() = checkFixByText(
        """
        module 0x1::m {
            fun main() {
                <weak_warning descr="Needless pair of `*` and `&` operators: consider removing them">*/*caret*/&1</weak_warning>;
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                1;
            }
        }        
    """
    )

    fun `test error for borrow and then deref with parens`() = checkFixByText(
        """
        module 0x1::m {
            fun main() {
                <weak_warning descr="Needless pair of `*` and `&` operators: consider removing them">*/*caret*/(&1)</weak_warning>;
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                1;
            }
        }        
    """
    )

    fun `test error for mutable borrow and then deref`() = checkFixByText(
        """
        module 0x1::m {
            fun main() {
                <weak_warning descr="Needless pair of `*` and `&` operators: consider removing them">*&mut /*caret*/1</weak_warning>;
            }
        }        
    ""","""
        module 0x1::m {
            fun main() {
                1;
            }
        }        
    """,
    )

    fun `test error for mutable borrow and then deref with parens`() = checkFixByText(
        """
        module 0x1::m {
            fun main() {
                <weak_warning descr="Needless pair of `*` and `&` operators: consider removing them">*(&mut /*caret*/1)</weak_warning>;
            }
        }        
    ""","""
        module 0x1::m {
            fun main() {
                1;
            }
        }        
    """,
    )

    fun `test error for mutable borrow and then deref with parens on item`() = checkFixByText(
        """
        module 0x1::m {
            fun main() {
                <weak_warning descr="Needless pair of `*` and `&` operators: consider removing them">*&mut (/*caret*/1)</weak_warning>;
            }
        }        
    ""","""
        module 0x1::m {
            fun main() {
                (1);
            }
        }        
    """,
    )

    private fun checkFixByText(
        @Language("Move") before: String,
        @Language("Move") after: String,
    ) = checkFixByText(
        "Remove needless `*`, `&` operators",
        before,
        after,
        checkWarn = false,
        checkWeakWarn = true
    )
}