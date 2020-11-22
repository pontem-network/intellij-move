package org.move.lang.parser

import org.move.utils.tests.parser.MoveParsingTestCase

class PartialParsingTest: MoveParsingTestCase("partial") {
    // top level items recovery
    fun `test top level items in script`() = doTest(true)
    fun `test module const`() = doTest(true)
    fun `test module uses`() = doTest(true)

    fun `test module spec`() = doTest(true)

    // functions
    fun `test function signatures`() = doTest(true)
    fun `test function calls`() = doTest(true)

    // structs
    fun `test struct fields`() = doTest(true)

    // expressions
//    fun `test expressions`() = doTest(true)
}