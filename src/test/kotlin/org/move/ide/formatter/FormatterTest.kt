package org.move.ide.formatter

import org.move.utils.tests.MvFormatterTestBase

class FormatterTest : MvFormatterTestBase() {
    fun `test blank lines`() = doTest()
    fun `test address block`() = doTest()
    fun `test structs`() = doTest()
    fun `test operators`() = doTest()
    fun `test functions`() = doTest()
    fun `test specs`() = doTest()
}
