package org.move.ide.intentions

import org.move.utils.tests.MvIntentionTestCase

class InlineAddressBlockIntentionTest: MvIntentionTestCase(InlineAddressBlockIntention::class) {
    fun `test convert address block into inline`() = doAvailableTest("""
    address 0x1 {
        module/*caret*/ M {}
    }    
    """, """
    module 0x1::M {/*caret*/}    
    """)
}
