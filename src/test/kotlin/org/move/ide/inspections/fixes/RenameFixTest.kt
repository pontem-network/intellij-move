package org.move.ide.inspections.fixes

import org.move.ide.inspections.MvUnusedVariableInspection
import org.move.utils.tests.annotation.InspectionTestBase

class RenameFixTest: InspectionTestBase(MvUnusedVariableInspection::class) {
    fun `test unused function parameter rename fix`() = checkFixByText(
        "Rename to _a", """
    module 0x1::M {
        fun call(<warning descr="Unused function parameter">/*caret*/a</warning>: u8): u8 {
            1
        } 
    }    
    """, """
    module 0x1::M {
        fun call(_a: u8): u8 {
            1
        } 
    }    
    """
    )

    fun `test unused variable`() = checkFixByText(
        "Rename to _a", """
    module 0x1::M {
        fun call(): u8 {
            let <warning descr="Unused variable">/*caret*/a</warning> = 1;
        } 
    }    
    """, """
    module 0x1::M {
        fun call(): u8 {
            let _a = 1;
        } 
    }    
    """
    )

    fun `test unused signer in unit test`() = checkFixByText(
        "Rename to _validator_acc", """
    module 0x1::M {
        #[test(validator_acc = @0x42)]
        fun test_function(<warning descr="Unused function parameter">/*caret*/validator_acc</warning>: signer) {
            
        }
    }
    """, """
    module 0x1::M {
        #[test(_validator_acc = @0x42)]
        fun test_function(_validator_acc: signer) {
            
        }
    }
    """
    )
}
