package org.move.lang.lexer

import org.move.utils.tests.MoveLexerTestCase

class CodeLexerTest: MoveLexerTestCase() {
    fun `test address blocks`() = doTest()
    fun `test address and script block`() = doTest()
    fun `test module`() = doTest()

    fun `test address identifier inside module`() = doTest()
    fun `test address identifier inside script`() = doTest()

    fun `test address named module`() = doTest()

    override fun getTestDataPath(): String = "org/move/lang/code_lexer"
}