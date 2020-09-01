package org.move.lang

import org.move.utils.tests.MoveLexerTestCase

class LexerTest: MoveLexerTestCase() {
    fun `test address block`() = doTest()
    fun `test address identifier inside module`() = doTest()
    fun `test address identifier inside script`() = doTest()
    fun `test in module spec keywords are identifiers`() = doTest()
    fun `test in spec keywords are keywords`() = doTest()
    fun `test global as keyword`() = doTest()
    fun `test global as function`() = doTest()
}