package org.move.ide.intentions

import org.move.utils.tests.MvIntentionTestCase

class ChopValueArgumentListIntentionTest: MvIntentionTestCase(ChopValueArgumentListIntention::class) {
    fun `test separate arguments in call expr`() = doAvailableTest("""
module 0x1::main {
    fun call(foo: u8, bar: u8, baz: u8) {}
    fun main() {
        call(/*caret*/1, 2, 3);
    }
}        
    """, """
module 0x1::main {
    fun call(foo: u8, bar: u8, baz: u8) {}
    fun main() {
        call(
            1,
            2,
            3
        );
    }
}        
    """)
}
