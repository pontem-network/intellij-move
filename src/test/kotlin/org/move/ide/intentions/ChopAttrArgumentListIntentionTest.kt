package org.move.ide.intentions

import org.move.utils.tests.MvIntentionTestCase

class ChopAttrArgumentListIntentionTest: MvIntentionTestCase(ChopAttrArgumentListIntention::class) {
    fun `test separate lines for test attributes`() = doAvailableTest("""
    module 0x1::M {
        #[test(/*caret*/signer1 = @0x1, signer2 = @0x2, signer3 = @0x3)]
        fun test_my_function() {
            
        }
    }
    """, """
    module 0x1::M {
        #[test(
            signer1 = @0x1,
            signer2 = @0x2,
            signer3 = @0x3
        )]
        fun test_my_function() {
            
        }
    }
    """)
}
