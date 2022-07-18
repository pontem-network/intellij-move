package org.move.ide.inspections

import org.move.utils.tests.annotation.InspectionTestBase

class UnusedTestSignerInspectionTest: InspectionTestBase(UnusedTestSignerInspection::class) {
    fun `test all signers are used`() = checkByText("""
    module 0x1::M {
        #[test(acc_1 = @0x1, acc_2 = @0x2)]
        fun test_a(acc_1: signer, acc_2: signer) {}
    }    
    """)

    fun `test no error in non test attr`() = checkByText("""
    module 0x1::M {
        #[test]
        #[expected_failure(abort_code = 1)]
        fun test_a() {}
    }    
    """)

    fun `test unused signer remove by quickfix at the end`() = checkFixByText("Remove 'acc_2'","""
    module 0x1::M {
        #[test(acc_1 = @0x1, <warning descr="Unused test signer">/*caret*/acc_2 = @0x2</warning>)]
        fun test_a(acc_1: signer) {}
    }    
    """, """
    module 0x1::M {
        #[test(acc_1 = @0x1)]
        fun test_a(acc_1: signer) {}
    }    
    """)

    fun `test unused signer remove by quickfix in the middle`() = checkFixByText("Remove 'acc_2'","""
    module 0x1::M {
        #[test(acc_1 = @0x1, <warning descr="Unused test signer">/*caret*/acc_2 = @0x2</warning>, acc_3 = @0x3)]
        fun test_a(acc_1: signer, acc_3: signer) {}
    }    
    """, """
    module 0x1::M {
        #[test(acc_1 = @0x1, acc_3 = @0x3)]
        fun test_a(acc_1: signer, acc_3: signer) {}
    }    
    """)

    fun `test unused signer remove by quickfix at the beginning`() = checkFixByText("Remove 'acc_2'","""
    module 0x1::M {
        #[test(<warning descr="Unused test signer">/*caret*/acc_2 = @0x2</warning>, acc_3 = @0x3)]
        fun test_a(acc_3: signer) {}
    }    
    """, """
    module 0x1::M {
        #[test(acc_3 = @0x3)]
        fun test_a(acc_3: signer) {}
    }    
    """)

    fun `test unused signer remove by quickfix multiline`() = checkFixByText("Remove 'acc2'","""
    module 0x1::M {
        #[test(
            acc1 = @0x1,
            <warning descr="Unused test signer">/*caret*/acc2 = @0x2</warning>, 
            acc3 = @0x3
        )]
        fun test_a(acc1: signer, acc3: signer) {}
    }    
    """, """
    module 0x1::M {
        #[test(
            acc1 = @0x1,
            acc3 = @0x3
        )]
        fun test_a(acc1: signer, acc3: signer) {}
    }    
    """)
}
