package org.move.utils.tests

import com.intellij.psi.formatter.FormatterTestCase
import org.intellij.lang.annotations.Language
import org.move.utils.tests.base.TestCase

abstract class MvFormatterTestCase : FormatterTestCase() {
    override fun getTestDataPath() = "src/test/resources"
    override fun getBasePath(): String = "org/move/ide/formatter.fixtures"
    override fun getFileExtension() = "move"

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }

    override fun doTextTest(@Language("Move") text: String, @Language("Move") textAfter: String) {
        check(text.trimIndent() != textAfter.trimIndent())
        super.doTextTest(text.trimIndent(), textAfter.trimIndent())
    }

}
