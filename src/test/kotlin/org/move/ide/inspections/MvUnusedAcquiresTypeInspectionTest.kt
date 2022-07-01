package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class MvUnusedAcquiresTypeInspectionTest : InspectionTestBase(MvUnusedAcquiresTypeInspection::class) {
    fun `test no error if used acquires type`() = checkWarnings(
        """
        module 0x1::M {
            struct S has key {}
            fun call() acquires S {
                borrow_global<S>(@0x1);
            }
        }
    """
    )

    fun `test error if unused acquires type`() = checkFixByText("Remove acquires",
        """
        module 0x1::M {
            struct S has key {}
            fun call() <warning descr="Unused acquires clause">/*caret*/acquires S</warning> {
            }
        }
    """, """
        module 0x1::M {
            struct S has key {}
            fun call() {
            }
        }
    """
    )

    fun `test error if duplicate acquires type`() = checkFixByText("Remove acquires",
        """
        module 0x1::M {
            struct S has key {}
            fun call() acquires S, <warning descr="Unused acquires clause">/*caret*/S</warning> {
                borrow_global<S>(@0x1);
            }
        }
    """, """
        module 0x1::M {
            struct S has key {}
            fun call() acquires S {
                borrow_global<S>(@0x1);
            }
        }
    """
    )
}
