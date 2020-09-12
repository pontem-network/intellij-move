package org.move.lang.parser

import org.move.utils.tests.parser.MoveParsingTestCase

class PartialParsingTest: MoveParsingTestCase("partial") {
    fun `test top level items in script`() = doTest(true)
//    fun `test assignments`() = doTest(true)
//    fun `test function calls`() = doTest(true)
}