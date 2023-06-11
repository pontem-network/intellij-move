package org.move.ide.intentions

import org.move.utils.tests.MvIntentionTestCase

class RemoveCurlyBracesIntentionTest: MvIntentionTestCase(RemoveCurlyBracesIntention::class) {
    fun `test remove curly braces simple`() = doAvailableTest(
        """
        module 0x1::m {
            use 0x1::Vector::{p/*caret*/ush};
        }
        """,
        """
        module 0x1::m {
            use 0x1::Vector::p/*caret*/ush;
        }
        """,
    )

    fun `test cannot remove curly braces if more than one item imported`() = doUnavailableTest(
        """
        module 0x1::m {
            use 0x1::Vector::{p/*caret*/ush, pull};
        }
        """
    )

    fun `test remove curly braces alias`() = doAvailableTest(
        """
        module 0x1::m {
            use 0x1::Vector::{p/*caret*/ush as p};
        }
        """,
        """
        module 0x1::m {
            use 0x1::Vector::p/*caret*/ush as p;
        }
        """,
    )
}
