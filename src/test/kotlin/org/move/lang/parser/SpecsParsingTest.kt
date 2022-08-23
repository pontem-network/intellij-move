package org.move.lang.parser

import org.move.utils.tests.parser.MvParsingTestCase

class SpecsParsingTest: MvParsingTestCase("specs") {
    fun `test scopes`() = doTest(true)
    fun `test conditions`() = doTest(true)
    fun `test forall exists`() = doTest(true)
    fun `test pragma`() = doTest(true)

    fun `test apply`() = doTest(true)
    fun `test spec statements`() = doTest(true)
    fun `test spec file`() = doTest(true)
}
