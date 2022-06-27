package org.move.ide.intentions

import org.move.utils.tests.MvIntentionTestCase

class ChopParameterListIntentionTest: MvIntentionTestCase(ChopParameterListIntention::class) {
    fun `test one parameter`() = doUnavailableTest("""
module 0x1::M {
    fun foo(/*caret*/p1: u64) {}
}        
    """)

    fun `test two parameter`() = doAvailableTest("""
        module 0x1::M {
            fun foo(/*caret*/p1: u64, p2: u64) {}
        }
    """, """
        module 0x1::M {
            fun foo(
                p1: u64,
                p2: u64
            ) {}
        }
    """)

    fun `test has all line breaks`() = doUnavailableTest("""
        module 0x1::M {
            fun foo(
                /*caret*/p1: u64,
                p2: u64,
                p3: u64
            ) {}
        }
    """)

    fun `test has some line breaks`() = doAvailableTest("""
        module 0x1::M {
            fun foo(p1: u64, /*caret*/p2: u64,
               p3: u64
            ) {}
        }
    """, """
        module 0x1::M {
            fun foo(
                p1: u64,
                p2: u64,
                p3: u64
            ) {}
        }
    """)

    fun `test has some line breaks 2`() = doAvailableTest("""
        module 0x1::M {
            fun foo(
                p1: u64, p2: u64, p3: u64/*caret*/
            ) {}
        }
    """, """
        module 0x1::M {
            fun foo(
                p1: u64,
                p2: u64,
                p3: u64
            ) {}
        }
    """)

    fun `test has comment`() = doUnavailableTest("""
        module 0x1::M {
            fun foo(
                /*caret*/p1: u64, /* comment */
                p2: u64,
                p3: u64
            ) {}
        }
    """)

    fun `test has comment 2`() = doAvailableTest("""
        module 0x1::M {
            fun foo(
                /*caret*/p1: u64, /*
                    comment
                */p2: u64,
                p3: u64
            ) {}
        }
    """, """
        module 0x1::M {
            fun foo(
                p1: u64, /*
                    comment
                */
                p2: u64,
                p3: u64
            ) {}
        }
    """)

    fun `test has single line comment`() = doAvailableTest("""
        module 0x1::M {
            fun foo(/*caret*/p1: u64, // comment p1
                   p2: u64, p3: u64 // comment p3
            ) {}
        }
    """, """
        module 0x1::M {
            fun foo(
                p1: u64, // comment p1
                p2: u64,
                p3: u64 // comment p3
            ) {}
        }
    """)

    fun `test trailing comma`() = doAvailableTest("""
        module 0x1::M {
            fun foo(/*caret*/p1: u64, p2: u64, p3: u64,) {}
        }
    """, """
        module 0x1::M {
            fun foo(
                p1: u64,
                p2: u64,
                p3: u64,
            ) {}
        }
    """)

    fun `test trailing comma with comments`() = doAvailableTest("""
        module 0x1::M {
            fun foo(/*caret*/p1: u64 /* comment 1 */, p2: u64, p3: u64 /* comment 2 */,) {}
        }
    """, """
        module 0x1::M {
            fun foo(
                p1: u64 /* comment 1 */,
                p2: u64,
                p3: u64 /* comment 2 */,
            ) {}
        }
    """)
}
