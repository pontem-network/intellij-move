package org.move.ide.inspections.acquires

import org.move.ide.inspections.MvAcquiresCheckInspection
import org.move.utils.tests.MoveV2
import org.move.utils.tests.annotation.InspectionTestBase

class MvUnusedAcquiresTypeInspectionTest: InspectionTestBase(MvAcquiresCheckInspection::class) {
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

    fun `test error if unused acquires type`() = checkWarnings(
        """
        module 0x1::M {
            struct S has key {}
            fun call() <warning descr="Unused acquires clause">/*caret*/acquires S</warning> {
            }
        }
    """)

    @MoveV2()
    fun `test no error if acquires type with receiver style`() = checkWarnings(
        """
        module 0x1::M {
            struct S has key {}
            fun acquire(self: &S) acquires S {
                borrow_global<S>(@0x1);
            }
            fun main(s: S) acquires S {
                s.acquire();
            }
        }
    """)

    fun `test error if redundant acquires with receiver style`() = checkWarnings(
        """
        module 0x1::M {
            struct S has key {}
            fun acquire(self: &S) {
            }
            fun main(s: S) <warning descr="Unused acquires clause">acquires S</warning> {
                s.acquire();
            }
        }
    """)

    fun `test error if duplicate acquires type`() = checkWarnings(
        """
        module 0x1::M {
            struct S has key {}
            fun call() acquires S, <warning descr="Unused acquires clause">/*caret*/S</warning> {
                borrow_global<S>(@0x1);
            }
        }
    """
    )

    fun `test warn if type not declared in the current module`() = checkWarnings(
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
    """
    )

    fun `test no unused acquires for borrow_global with dot expr`() = checkWarnings(
        """
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    fun get_lockup_secs(pool_address: address): u64 acquires StakePool {
        borrow_global<StakePool>(pool_address).locked_until_secs
    }
}        
    """
    )

    fun `test no unused acquires with inline function`() = checkWarnings(
        """
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    fun get_lockup_secs(pool_address: address) acquires StakePool {
        f();
    }
    inline fun f() {
        borrow_global<StakePool>(pool_address);
    }
}        
    """
    )

    fun `test no unused acquires if declared on inline function`() = checkWarnings(
        """
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    inline fun get_lockup_secs(pool_address: address) acquires StakePool {
        f();
    }
    inline fun f() {
        borrow_global<StakePool>(pool_address);
    }
}        
    """
    )

    fun `test error if declared on inline function but not acquired`() = checkWarnings(
        """
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    inline fun get_lockup_secs(pool_address: address) <warning descr="Unused acquires clause">acquires StakePool</warning> {
    }
}        
    """
    )

    fun `test no unused acquires if declared on inline function but not acquired nested`() = checkWarnings(
        """
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    inline fun get_lockup_secs(pool_address: address) <warning descr="Unused acquires clause">acquires StakePool</warning> {
        f();
    }
    inline fun f() {
    }
}        
    """
    )

//    @MoveV2(false)
//    fun `test unused acquires with index expr on compiler v1`() = checkWarnings(
//        """
//module 0x1::main {
//    struct StakePool has key {
//        locked_until_secs: u64,
//    }
//    fun get_lockup_secs(pool_address: address) <warning descr="Unused acquires clause">acquires StakePool</warning> {
//        StakePool[pool_address];
//    }
//}
//    """
//    )

    @MoveV2()
    fun `test no unused acquires with index expr`() = checkWarnings(
        """
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    fun get_lockup_secs(pool_address: address) acquires StakePool {
        StakePool[pool_address];
    }
}        
    """
    )

    @MoveV2()
    fun `test no unused acquires with index expr inside inline function`() = checkWarnings(
        """
module 0x1::main {
    struct StakePool has key {
        locked_until_secs: u64,
    }
    fun get_lockup_secs(pool_address: address) acquires StakePool {
        f();
    }
    inline fun f() {
        StakePool[pool_address];
    }
}        
    """
    )

    fun `test no unused acquires for deep borrow global dot`() = checkWarnings(
        """
module 0x1::main {
    /// The enabled features, represented by a bitset stored on chain.
    struct Features has key {
        features: vector<u8>,
    }
    
    #[view]
    /// Check whether the feature is enabled.
    public fun is_enabled(feature: u64): bool acquires Features {
        exists<Features>(@0x1) &&
            contains(&borrow_global<Features>(@0x1).features, feature)
    }
}        
    """
    )

    @MoveV2()
    fun `test no unused acquires for deep borrow global dot with index expr`() = checkWarnings(
        """
module 0x1::main {
    /// The enabled features, represented by a bitset stored on chain.
    struct Features has key {
        features: vector<u8>,
    }
    #[view]
    /// Check whether the feature is enabled.
    public fun is_enabled(feature: u64): bool acquires Features {
        exists<Features>(@0x1) && 
            contains(Features[@0x1].features, feature)
    }
}        
    """
    )
}
