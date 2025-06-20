package org.move.ide.inspections

import org.move.utils.tests.MoveV2
import org.move.utils.tests.DebugMode
import org.move.utils.tests.NamedAddress
import org.move.utils.tests.annotation.InspectionTestBase

class MvUnresolvedReferenceInspectionTest : InspectionTestBase(MvUnresolvedReferenceInspection::class) {
    fun `test unresolved variable`() = checkByText("""
        module 0x1::M {
            fun main() {
                <error descr="Unresolved reference: `x`">x</error>;
            }
        }
    """)

    fun `test unresolved function call`() = checkByText("""
        module 0x1::M {
            fun main() {
                <error descr="Unresolved function: `call`">call</error>();
            }
        }
    """)

    fun `test unresolved module member`() = checkByText("""
        script {
            use 0x1::<error descr="Unresolved reference: `Module`">Module</error>::call;

            fun main() {
                <error descr="Unresolved function: `call`">call</error>();
            }
        }
    """)

    fun `test test no unresolved reference for builtin`() = checkByText("""
        module 0x1::M {
            fun main() {
                move_from<u8>(@0x1);
            }
        }
    """)

    fun `test test no unresolved reference for primitive type`() = checkByText("""
        script {
            fun main(s: &signer) {
            }
        }
    """)

    fun `test unresolved reference to variable in struct constructor field`() = checkByText(
        """
        module 0x1::M {
            struct T {
                my_field: u8
            }

            fun main() {
                let t = T { my_field: <error descr="Unresolved reference: `my_unknown_field`">my_unknown_field</error> };
            }
        }
    """
    )

    fun `test no error for field shorthand`() = checkByText(
        """
        module 0x1::M {
            struct T {
                my_field: u8
            }

            fun main() {
                let my_field = 1;
                let t = T { my_field };
            }
        }
    """
    )

    fun `test unresolved reference to field in struct constructor`() = checkByText(
        """
        module 0x1::M {
            struct T {
                my_field: u8
            }

            fun main() {
                let t = T { <error descr="Unresolved field: `my_unknown_field`">my_unknown_field</error>: 1 };
            }
        }
    """
    )

    fun `test unresolved reference to field in struct pat`() = checkByText(
        """
        module 0x1::M {
            struct T {
                my_field: u8
            }

            fun main() {
                let T { <error descr="Unresolved field: `my_unknown_field`">my_unknown_field</error>: _ } = T { };
            }
        }
    """
    )

    fun `test unresolved reference to field in struct pat shorthand`() = checkByText(
        """
        module 0x1::M {
            struct T {
                my_field: u8
            }

            fun main() {
                let T { <error descr="Unresolved field: `my_unknown_field`">my_unknown_field</error> } = T { };
            }
        }
    """
    )

    fun `test unresolved reference to module`() = checkByText(
        """
        module 0x1::M {
            fun main() {
                let t = <error descr="Unresolved reference: `Transaction`">Transaction</error>::create();
            }
        }
    """
    )

    fun `test unresolved reference for fully qualified module`() = checkByText(
        """
        module 0x1::M {
            fun main() {
                0x1::<error descr="Unresolved reference: `Debug`">Debug</error>::print(1);
            }
        }
    """
    )

    fun `test unresolved reference for method of another module`() = checkByText(
        """
    module 0x1::other {}
    module 0x1::m {
        use 0x1::other;
        fun main() {
            other::<error descr="Unresolved function: `emit`">emit</error>();
        }
    }
        """
    )

    fun `test unresolved reference to type in generic`() = checkByText(
        """
        module 0x1::m {
            fun deposit<Token> () {}

            fun main() {
                deposit<<error descr="Unresolved type: `PONT`">PONT</error>>()
            }
        }    
        """
    )

    fun `test no unresolved reference for _ in destructuring pattern`() = checkByText("""
    module 0x1::M {
        struct Coin { value: u64 }
        fun call(): Coin { Coin { value: 1 } }
        fun m() {
            Coin { value: _ } = call();
        }
    }    
    """)

    fun `test no unresolved reference for correct destructuring`() = checkByText("""
    module 0x1::M {
        struct Coin { value: u64 }
        fun call(): Coin { Coin { value: 1 } }
        fun m() {
            let val;
            Coin { value: val } = call();
        }
    }    
    """)

    fun `test unresolved reference for unbound destructured value`() = checkByText("""
    module 0x1::M {
        struct Coin { value: u64 }
        fun call(): Coin { Coin { value: 1 } }
        fun m() {
            Coin { value: <error descr="Unresolved reference: `val`">val</error> } = call();
        }
    }    
    """)

    fun `test no unresolved reference for result variable in specs`() = checkByText("""
    module 0x1::M {
        fun call(): u8 { 1 }
        spec call {
            ensures result >= 1;
        }
    }    
    """)

    fun `test unresolved reference for schema field`() = checkByText("""
    module 0x1::M {
        spec schema Schema {}
        spec module {
            include Schema { <error descr="Unresolved field: `addr`">addr</error>: @0x1 };
        }
    }
    """)

    fun `test unresolved reference for schema field shorthand`() = checkByText("""
    module 0x1::M {
        spec schema Schema {}
        spec module {
            let addr = @0x1;
            include Schema { <error descr="Unresolved field: `addr`">addr</error> };
        }
    }        
    """)

    fun `test no unresolved reference for schema field and function param`() = checkByText("""
    module 0x1::M {
        spec schema Schema { 
            root_account: signer;
        }
        fun call(root_account: &signer) {}
        spec call {
            include Schema { root_account };
        }
    }        
    """)

    fun `test result_1 result_2 is special variables for tuple return type`() = checkByText("""
    module 0x1::M {
        fun call(): (u8, u8) { (1, 1) }
        spec call {
            ensures result_1 == result_2
        }
    }    
    """)

    fun `test second argument to update field`() = checkByText("""
    module 0x1::M {
        struct S { val: u8 }
        spec module {
            let s = S { val: 1 };
            ensures update_field(s, val, s.val + 1) == 1; 
        }
    }    
    """)

    fun `test num type`() = checkByText("""
    module 0x1::M {
        spec schema SS {
            val: num;
        }
    }    
    """)

    fun `test unresolved field for dot expression`() = checkByText(
        """
    module 0x1::M {
        struct S has key {}
        fun call() acquires S {
            let a = borrow_global_mut<S>(@0x1);
            a.<error descr="Unresolved field: `val`">val</error>;
        }
    }    
    """
    )

    fun `test unresolved module import`() = checkByText("""
    module 0x1::main {
        use 0x1::<error descr="Unresolved reference: `M1`">M1</error>;
    }    
    """)

    fun `test unresolved module import in item import`() = checkByText("""
    module 0x1::Main {
        use 0x1::<error descr="Unresolved reference: `M1`">M1</error>::call;
    }    
    """)

    fun `test unresolved item import`() = checkByText("""
    module 0x1::M1 {}    
    module 0x1::Main {
        use 0x1::M1::<error descr="Unresolved reference: `call`">call</error>;
    }    
    """)

    // ?

    @DebugMode(false)
    fun `test no error for dot field in specs without development mode`() = checkByText("""
module 0x1::main {
    struct S {}
    fun call(): S {}
    fun main() {}
}   
spec 0x1::main {
    spec main {
        call().val
    }
}
    """)

    fun `test no error for field of item of unknown type`() = checkByText("""
module 0x1::main {
    fun main() {
        let var = (1 + false);
        var.key;
    }
}        
    """)

    fun `test no error for field of reference of unknown type`() = checkByText("""
module 0x1::main {
    fun call<T>(t: T): &T { &t }
    fun main() {
        let var = &(1 + false);
        var.key;
    }
}        
    """)

    fun `test no unresolved reference for named address in location`() = checkByText("""
#[test_only]        
module 0x1::string_tests {
    #[expected_failure(location = aptos_framework::coin)]
    fun test_abort() {
    }
}        
    """)

    fun `test no error for self module in location`() = checkByText("""
#[test_only]
module 0x1::string_tests {
    #[test]
    #[expected_failure(location = Self)]
    fun test_a() {

    }
}        
    """)

    fun `test lhs of dot assignment`() = checkByText("""
module 0x1::mod {
    struct S { val: u8 }
    fun main() {
        <error descr="Unresolved reference: `s`">s</error>.val = 1;
    }
}
    """)

    fun `test no unresolved reference for attribute items`() = checkByText("""
module 0x1::m {
    #[resource_group(scope = global)]
    /// A shared resource group for storing object resources together in storage.
    struct ObjectGroup { }
}        
    """)

    fun `test spec builtin const unresolved outside spec`() = checkByText("""
module 0x1::m {
    fun main() {
        <error descr="Unresolved reference: `MAX_U128`">MAX_U128</error>;
    }
}
    """)

    fun `test spec builtin const in spec`() = checkByText("""
module 0x1::m {
    fun main() {
        spec {
            MAX_U128;
        }
    }
}
    """)

    // ?

    fun `test result variable for return type tuple in function spec`() = checkByText("""
        module 0x1::m {
            public fun get_fees_distribution(): (u128, u128) {
                (1, 1)
            }
            spec get_fees_distribution {
                aborts_if false;
                ensures result_1 == 1;
                ensures result_2 == 1;
                ensures <error descr="Unresolved reference: `result_3`">result_3</error> == 1;
            }
        }        
    """)

    fun `test no unresolved reference in pragma`() = checkByText("""
        module 0x1::m {
            spec module {
                pragma intrinsic = map;
            }
        }        
    """)

    // TODO: test
//    fun `test field inaccessible outside of the defining module`() = checkByText("""
//        module 0x1::m {
//            struct S { my_field: u8 }
//        }
//        module 0x1::main {
//            use 0x1::m::S;
//            fun main(s: &mut S) {
//                s.<error descr="Unresolved field: 'my_field' is inaccessible outside of the defining module">my_field</error>;
//            }
//        }
//    """)

    @NamedAddress("std", "0x1")
    fun `test no unresolved reference for named address in use`() = checkByText("""
        module std::m {
        }
        module std::main {
            use std::m;
        }
    """)

    @NamedAddress("std", "0x1")
    fun `test no unresolved reference for named address in fq`() = checkByText("""
        module std::mymodule {
            public fun call() {}
        }
        module 0x1::main {
            fun main() {
                std::mymodule::call();
            }
        }         
    """)

    fun `test no error for invariant index variable`() = checkByText(
        """
        module 0x1::m {
            spec module {
                let vec = vector[1, 2, 3];
                let ind = 1;
                invariant forall ind in 0..10: vec[ind] < 10;
            }
        }        
    """
    )

    // ?

    @MoveV2(enabled = false)
    fun `test no unresolved method in compiler v1`() = checkByText("""
        module 0x1::m {
            struct S { field: u8 }
            fun main(s: S) {
                s.receiver();                
            }
        }        
    """)

    @MoveV2()
    fun `test unresolved method`() = checkByText("""
        module 0x1::m {
            struct S { field: u8 }
            fun main(s: S) {
                s.<error descr="Unresolved reference: `receiver`">receiver</error>();                
            }
        }        
    """)

    @MoveV2()
    fun `test no unresolved method error`() = checkByText("""
        module 0x1::m {
            struct S { field: u8 }
            fun receiver(self: S): u8 { self.field }
            fun main(s: S) {
                s.receiver();
            }
        }        
    """)

    @MoveV2
    fun `test no error if receiver is type unknown`() = checkByText("""
        module 0x1::m {
            struct S { field: u8 }
            fun receiver(self: S): u8 { self.field }
            fun main() {
                let t = &(1 + false);
                t.receiver();
            }
        }        
    """)

    @MoveV2
    fun `test no error for fields if destructuring unknown struct`() = checkByText("""
        module 0x1::m {
            fun main() {
                let <error descr="Unresolved reference: `S`">S</error> { val } = 1;
                let <error descr="Unresolved reference: `S`">S</error>(val) = 1;
            }
        }        
    """)

    @MoveV2
    fun `test no error for fields if destructuring unknown struct with qualifier`() = checkByText("""
        module 0x1::m {
            enum R {}
            fun main() {
                let R::<error descr="Unresolved reference: `Inner`">Inner</error> { val } = 1;
                let R::<error descr="Unresolved reference: `Inner`">Inner</error>(val) = 1;
            }
        }        
    """)

    fun `test no error for path in attr`() = checkByText("""
        module 0x1::m {
            #[lint::my_lint]
            fun main() {}
        }        
    """)

    // **

    fun `test no error for unknown receiver method of result of unknown resource borrow`() = checkByText("""
        module 0x1::m {
            fun main() {
                let perm_storage = &<error descr="Unresolved reference: `PermissionStorage`">PermissionStorage</error>[@0x1];
                perm_storage.contains();
            }
        }        
    """)

    fun `test no error for unknown receiver method of result of unknown mut resource borrow`() = checkByText("""
        module 0x1::m {
            fun main() {
                let perm_storage = &mut <error descr="Unresolved reference: `PermissionStorage`">PermissionStorage</error>[@0x1];
                perm_storage.contains();
            }
        }        
    """)

    @NamedAddress("uq64x64", "0x1")
    fun `test no error on module for unresolved module if the same name as address`() = checkByText("""
        module 0x1::m {
            fun main() {
                uq64x64::call();
            }
        }
    """)

    @NamedAddress("uq64x64", "0x2")
    fun `test error on known item of module with the same name as address`() = checkByText("""
        module uq64x64::uq64x64 {
        }
        module 0x1::m {
            use uq64x64::uq64x64;
            fun main() {
                uq64x64::<error descr="Unresolved function: `call`">call</error>();
            }
        }
    """)

    fun `test no unresolved reference for const in spec`() = checkByText("""
        module 0x1::features {
            const PERMISSIONED_SIGNER: u64 = 84;
            
        }
        module 0x1::m {}
        spec 0x1::m {
            spec fun is_permissioned_signer(): bool {
                use 0x1::features::PERMISSIONED_SIGNER;
                PERMISSIONED_SIGNER;
            }
        }
    """)
}
