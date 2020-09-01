package org.move.lang.parser

import org.move.utils.tests.parser.MoveParsingTestCase

class SpecsParsingTest: MoveParsingTestCase("specs") {
    fun `test scopes`() = doTest(true)
}