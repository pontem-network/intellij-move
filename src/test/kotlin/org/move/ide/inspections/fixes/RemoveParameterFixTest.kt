package org.move.ide.inspections.fixes

import org.move.ide.inspections.MvUnusedVariableInspection
import org.move.utils.tests.annotation.InspectionTestBase

class RemoveParameterFixTest : InspectionTestBase(MvUnusedVariableInspection::class) {
    fun `test single parameter`() = checkFixByText(
        "Remove parameter", """
    module 0x1::M {
        fun call(<warning descr="Unused function parameter">/*caret*/a</warning>: u8): u8 {
            1
        }
        fun main() {
            call(1);
        }
    }    
    """, """
    module 0x1::M {
        fun call(): u8 {
            1
        }
        fun main() {
            call();
        }
    }    
    """
    )

    fun `test first of two`() = checkFixByText(
        "Remove parameter", """
    module 0x1::M {
        fun call(<warning descr="Unused function parameter">/*caret*/a</warning>: u8, b: u8): u8 {
            b
        }
        fun main() {
            call(1, 2);
        }
    }    
    """, """
    module 0x1::M {
        fun call(b: u8): u8 {
            b
        }
        fun main() {
            call(2);
        }
    }    
    """
    )

    fun `test first of two multiline`() = checkFixByText(
        "Remove parameter", """
    module 0x1::M {
        fun call(
            <warning descr="Unused function parameter">/*caret*/a</warning>: u8, 
            b: u8
        ): u8 {
            b
        }
        fun main() {
            call(
                1, 
                2
            );
        }
    }    
    """, """
    module 0x1::M {
        fun call(
            b: u8
        ): u8 {
            b
        }
        fun main() {
            call(
                2
            );
        }
    }    
    """
    )

//    fun `test second of two multiline`() = checkFixByText(
//        "Remove parameter", """
//    module 0x1::M {
//        fun call(
//            b: u8,
//            <warning descr="Unused function parameter">/*caret*/a</warning>: u8,
//        ): u8 {
//            b
//        }
//        fun main() {
//            call(
//                1,
//                2
//            );
//        }
//    }
//    """, """
//    module 0x1::M {
//        fun call(
//            b: u8
//        ): u8 {
//            b
//        }
//        fun main() {
//            call(
//                1
//            );
//        }
//    }
//    """
//    )

    fun `test second of three`() = checkFixByText(
        "Remove parameter", """
    module 0x1::M {
        fun call(x: u8, <warning descr="Unused function parameter">/*caret*/a</warning>: u8, b: u8): u8 {
            x + b
        }
        fun main() {
            call(0, 1, 2);
        }
    }
    """, """
    module 0x1::M {
        fun call(x: u8, b: u8): u8 {
            x + b
        }
        fun main() {
            call(0,  2);
        }
    }
    """
    )

    fun `test second of three multiline`() = checkFixByText(
        "Remove parameter", """
    module 0x1::M {
        fun call(
            x: u8, 
            <warning descr="Unused function parameter">/*caret*/a</warning>: u8, 
            b: u8
        ): u8 {
            x + b
        }
        fun main() {
            call(
                0, 
                1, 
                2
            );
        }
    }    
    """, """
    module 0x1::M {
        fun call(
            x: u8,
            b: u8
        ): u8 {
            x + b
        }
        fun main() {
            call(
                0,
                2
            );
        }
    }    
    """
    )

    fun `test remove unused test signer`() = checkFixByText(
        "Remove parameter",
        """
    module 0x1::main {
        #[test(harvest = @harvest, alice = @alice)]
        fun test_end_to_end(<warning descr="Unused function parameter">/*caret*/harvest</warning>: signer, alice: signer) {
            address_of(alice);
        }
    }            
    """, """
    module 0x1::main {
        #[test(alice = @alice)]
        fun test_end_to_end(alice: signer) {
            address_of(alice);
        }
    }            
    """
    )

    fun `test remove unused only test signer`() = checkFixByText(
        "Remove parameter",
        """
    module 0x1::main {
        #[test(harvest = @harvest)]
        fun test_end_to_end(<warning descr="Unused function parameter">/*caret*/harvest</warning>: signer) {
        }
    }            
    """, """
    module 0x1::main {
        #[test]
        fun test_end_to_end() {
        }
    }            
    """
    )
}
