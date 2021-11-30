package org.move.ide.intentions

import org.move.utils.tests.MoveIntentionTestCase

class RemoveCurlyBracesIntentionTest: MoveIntentionTestCase(RemoveCurlyBracesIntention::class) {
    fun `test remove curly braces simple`() = doAvailableTest(
        """
        module M {
            use 0x1::Vector::{p/*caret*/ush};
        }
        """,
        """
        module M {
            use 0x1::Vector::p/*caret*/ush;
        }
        """,
    )

    fun `test remove curly braces alias`() = doAvailableTest(
        """
        module M {
            use 0x1::Vector::{p/*caret*/ush as p};
        }
        """,
        """
        module M {
            use 0x1::Vector::p/*caret*/ush as p;
        }
        """,
    )
}
