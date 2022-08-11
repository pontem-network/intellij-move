package org.move.ide.intentions

import org.move.utils.tests.MvIntentionTestCase

class ChopStructLiteralIntentionTest: MvIntentionTestCase(ChopStructLiteralIntention::class) {
    fun `test separate lines for test attributes`() = doAvailableTest("""
    module 0x1::M {
        struct S { val1: u8, val2: u8 }
        fun test_my_function() {
            S { /*caret*/val1: u8, val2: u8 };
        }
    }
    """, """
    module 0x1::M {
        struct S { val1: u8, val2: u8 }
        fun test_my_function() {
            S {
                val1: u8,
                val2: u8
            };
        }
    }
    """)
}
