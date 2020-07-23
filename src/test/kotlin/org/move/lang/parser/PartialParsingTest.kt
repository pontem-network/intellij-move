package org.move.lang.parser

import org.move.utils.tests.MvParsingTestCase

class PartialParsingTest: MvParsingTestCase("partial") {
    fun `test empty script`() = doTest(true)
    fun `test assignments`() = doTest(true)
    fun `test function calls`() = doTest(true)
}