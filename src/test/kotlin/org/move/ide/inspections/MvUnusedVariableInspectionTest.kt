package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class MvUnusedVariableInspectionTest : InspectionTestBase(MvUnusedVariableInspection::class) {
    fun `test used function parameter`() = checkByText(
        """
    module 0x1::M {
        fun call(a: u8): u8 {
            a + 1
        } 
    }    
    """
    )

    fun `test used variable`() = checkByText(
        """
    module 0x1::M {
        fun call(): u8 {
            let a = 1;
            a + 1
        } 
    }    
    """
    )

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

    fun `test no error if prefixed with underscore`() = checkByText(
        """
    module 0x1::M {
        fun call(_a: u8) {
            let _b = 1;
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

    fun `test no unused parameter for native function`() = checkByText("""
module 0x1::main {
    native fun main(a: u8);
}        
    """)

    fun `test no unused parameter on uninterpreted spec function`() = checkByText("""
spec 0x1::main {
    // An uninterpreted spec function to represent the stake reward formula.
    spec fun spec_rewards_amount(
        stake_amount: u64,
        num_successful_proposals: u64,
        num_total_proposals: u64,
        rewards_rate: u64,
        rewards_rate_denominator: u64,
    ): u64;
}        
    """)
}
