package org.move.lang.parser

import org.move.utils.tests.parser.MoveParsingTestCase

class PartialParsingTest: MoveParsingTestCase("partial") {
    // top level items recovery
    fun `test top level items in script`() = doTest(true)
    fun `test top level items in module`() = doTest(true)
    fun `test module const`() = doTest(true)
//    fun `test assignments`() = doTest(true)
//    fun `test function calls`() = doTest(true)
}