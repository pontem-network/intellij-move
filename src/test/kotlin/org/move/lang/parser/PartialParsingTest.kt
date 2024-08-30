package org.move.lang.parser

import org.move.utils.tests.parser.MvParsingTestCase

class PartialParsingTest: MvParsingTestCase("partial") {
    // top level items recovery
    fun `test top level items`() = doTest(true)
    fun `test module const`() = doTest(true)
//    fun `test module uses`() = doTest(true)

    fun `test spec`() = doTest(true)

    // functions
    fun `test function signatures`() = doTest(true)
    fun `test function calls`() = doTest(true)

    // structs
    fun `test struct decls`() = doTest(true)
    fun `test struct fields`() = doTest(true)

    // expressions
    fun `test expressions`() = doTest(true)
    fun `test assignments`() = doTest(true)
    fun `test dot exprs`() = doTest(true)

    fun `test enum match`() = doTest(true)
}
