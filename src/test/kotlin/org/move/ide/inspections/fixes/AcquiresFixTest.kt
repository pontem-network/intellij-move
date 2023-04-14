package org.move.ide.inspections.fixes

import org.move.ide.inspections.MvMissingAcquiresInspection
import org.move.utils.tests.annotation.InspectionTestBase

class AcquiresFixTest : InspectionTestBase(MvMissingAcquiresInspection::class) {
    fun `test one item error with fix`() = checkFixByText(
        "Add missing `acquires Loan`", """
    module 0x1::M {
        struct Loan has key {}
        fun main() {
            <error descr="Function 'main' is not marked as 'acquires Loan'">move_from<Loan>/*caret*/(@0x1)</error>;
        }
    }    
    """, """
    module 0x1::M {
        struct Loan has key {}
        fun main() acquires Loan {
            move_from<Loan>/*caret*/(@0x1);
        }
    }    
    """
    )

    fun `test two items error with fix`() = checkFixByText(
        "Add missing `acquires Deal, Loan`", """
    module 0x1::M {
        struct Loan has key {}
        struct Deal has key {}
        fun call() acquires Deal, Loan {
            move_from<Loan>(@0x1);
            move_from<Deal>(@0x1);
        }
        fun main() {
            <error descr="Function 'main' is not marked as 'acquires Deal, Loan'">call/*caret*/()</error>;
        }
    }    
    """, """
    module 0x1::M {
        struct Loan has key {}
        struct Deal has key {}
        fun call() acquires Deal, Loan {
            move_from<Loan>(@0x1);
            move_from<Deal>(@0x1);
        }
        fun main() acquires Deal, Loan {
            call/*caret*/();
        }
    }    
    """
    )

    fun `test add acquires if present with generic`() = checkFixByText(
        "Add missing `acquires CapState`", """
    module 0x1::M {
        struct CapState<phantom Feature> has key {}
        fun main<Feature>() {
            <error descr="Function 'main' is not marked as 'acquires CapState'">move_from<CapState<Feature>>/*caret*/(@0x1)</error>;
        }  
    }    
    """, """
    module 0x1::M {
        struct CapState<phantom Feature> has key {}
        fun main<Feature>() acquires CapState {
            move_from<CapState<Feature>>/*caret*/(@0x1);
        }  
    }    
    """
    )
}
