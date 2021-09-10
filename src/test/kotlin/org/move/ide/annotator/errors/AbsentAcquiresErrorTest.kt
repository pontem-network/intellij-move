package org.move.ide.annotator.errors

import org.move.ide.annotator.ErrorAnnotator
import org.move.utils.tests.annotation.AnnotatorTestCase

class AbsentAcquiresErrorTest: AnnotatorTestCase(ErrorAnnotator::class) {
    fun `test move_from called and no acquires present`() = checkErrors("""
    module M {
        struct Loan has key {}
        fun call() {
            <error descr="Function 'call' is not marked as 'acquires Loan'">move_from<Loan>(@0x1)</error>;
            <error descr="Function 'call' is not marked as 'acquires Loan'">borrow_global<Loan>(@0x1)</error>;
            <error descr="Function 'call' is not marked as 'acquires Loan'">borrow_global_mut<Loan>(@0x1)</error>;
        }
    }    
    """)

    fun `test no error if acquires present`() = checkErrors("""
    module M {
        struct Loan has key {}
        fun call() acquires Loan {
            move_from<Loan>(@0x1);
            borrow_global<Loan>(@0x1);
            borrow_global_mut<Loan>(@0x1);
        }
    }    
    """)
}
