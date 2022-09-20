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

    fun `test warn if type not declared in the current module`() = checkFixByText("Remove acquires",
        """
        module 0x1::M {
            struct S has key {}
            public fun call() acquires S {
                borrow_global<S>(@0x1);
            }
        }
        module 0x1::M2 {
            use 0x1::M::{Self, S};
            fun call() <warning descr="Unused acquires clause">/*caret*/acquires S</warning> {
                M::call();        
            }
        }
    """, """
        module 0x1::M {
            struct S has key {}
            public fun call() acquires S {
                borrow_global<S>(@0x1);
            }
        }
        module 0x1::M2 {
            use 0x1::M::{Self, S};
            fun call() {
                M::call();        
            }
        }
    """
    )

    fun `test no unused acquires for borrow_global with dot expr`() = checkWarnings("""
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    fun get_lockup_secs(pool_address: address): u64 acquires StakePool {
        borrow_global<StakePool>(pool_address).locked_until_secs
    }
}        
    """)

    fun `test no unused acquires with `() = checkWarnings("""
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    fun get_lockup_secs(pool_address: address) acquires StakePool {
        borrow_global<StakePool>(pool_address);
    }
}        
    """)
}
