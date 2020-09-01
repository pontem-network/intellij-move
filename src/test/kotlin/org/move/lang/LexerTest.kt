package org.move.lang

import org.move.utils.tests.MoveLexerTestCase

class LexerTest: MoveLexerTestCase() {
    fun `test address block`() = doTest()
    fun `test address identifier inside module`() = doTest()
    fun `test address identifier inside script`() = doTest()
    fun `test address named module`() = doTest()
}