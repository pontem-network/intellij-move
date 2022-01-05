package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionsTestCase

class MvMissingAcquiresInspectionTest: InspectionsTestCase(MvMissingAcquiresInspection::class) {
    fun `test move_from called and no acquires present`() = checkErrors("""
    module 0x1::M {
        struct Loan has key {}
        fun call() {
            <error descr="Function 'call' is not marked as 'acquires Loan'">move_from<Loan>(@0x1)</error>;
            <error descr="Function 'call' is not marked as 'acquires Loan'">borrow_global<Loan>(@0x1)</error>;
            <error descr="Function 'call' is not marked as 'acquires Loan'">borrow_global_mut<Loan>(@0x1)</error>;
        }
    }    
    """)

    fun `test no error if acquires present`() = checkErrors("""
    module 0x1::M {
        struct Loan has key {}
        fun call() acquires Loan {
            move_from<Loan>(@0x1);
            borrow_global<Loan>(@0x1);
            borrow_global_mut<Loan>(@0x1);
        }
    }    
    """)

    fun `test no error if acquires with generic`() = checkErrors("""
    module 0x1::M {
        struct CapState<phantom Feature> has key {}
        fun m<Feature>() acquires CapState {
            borrow_global_mut<CapState<Feature>>(@0x1);
        }
    }    
    """)
}
