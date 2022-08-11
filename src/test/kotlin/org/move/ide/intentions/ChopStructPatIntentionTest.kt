package org.move.ide.intentions

import org.move.utils.tests.MvIntentionTestCase

class ChopStructPatIntentionTest: MvIntentionTestCase(ChopStructPatIntention::class) {
    fun `test separate lines for test attributes`() = doAvailableTest("""
    module 0x1::M {
        struct S { val1: u8, val2: u8 }
        fun test_my_function() {
            let S { val1: u8, /*caret*/val2: u8 } = call();
        }
    }
    """, """
    module 0x1::M {
        struct S { val1: u8, val2: u8 }
        fun test_my_function() {
            let S {
                val1: u8,
                val2: u8
            } = call();
        }
    }
    """)
}
