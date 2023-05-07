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

    fun `test no error if prefixed with underscore`() = checkByText(
        """
    module 0x1::M {
        fun call(_a: u8) {
            let _b = 1;
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

    // TODO: later
    fun `test variable used as implicit schema parameter`() = checkByText("""
        module 0x1::m {
            fun call() {}
            spec schema MySchema { 
                account_addr: address;
            }
            spec call {
                let account_addr = @0x1;
                include MySchema;
            }
        }        
    """)

    // TODO: later
    fun `test variable used as implicit schema parameter with existing fields`() = checkByText("""
        module 0x1::m {
            fun call() {}
            spec schema MySchema { 
                account_addr: address;
                amount: u8;
            }
            spec call {
                let account_addr = @0x1;
                include MySchema { amount: 100 };
            }
        }        
    """)
}
