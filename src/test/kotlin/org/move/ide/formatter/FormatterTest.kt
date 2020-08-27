package org.move.ide.formatter

import org.move.utils.tests.MoveFormatterTestCase

class FormatterTest : MoveFormatterTestCase() {
    fun `test toplevel blocks`() = doTest()
    fun `test structs`() = doTest()
    fun `test operators`() = doTest()
    fun `test functions`() = doTest()
    fun `test blank lines`() = doTest()
}