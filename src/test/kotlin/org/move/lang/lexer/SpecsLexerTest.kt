package org.move.lang.lexer

import org.move.utils.tests.MoveLexerTestCase

class SpecsLexerTest : MoveLexerTestCase() {
    fun `test in module spec keywords are identifiers`() = doTest()
    fun `test in spec keywords are keywords`() = doTest()
    fun `test global as keyword`() = doTest()
    fun `test global as function`() = doTest()
    fun `test line comment inside spec block`() = doTest()

    fun `test specs and address`() = doTest()
    fun `test specs and initial keywords`() = doTest()

    override fun getTestDataPath(): String = "org/move/lang/specs_lexer"
}