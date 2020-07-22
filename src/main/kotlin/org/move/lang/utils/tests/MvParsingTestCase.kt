package org.move.lang.utils.tests

import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.annotations.NonNls
import org.move.lang.MoveParserDefinition

abstract class MvParsingTestCase(@NonNls dataPath: String) : ParsingTestCase(
    "org/move/lang/parser/$dataPath",
    "move",
    true,
    MoveParserDefinition()
) {
    override fun getTestDataPath(): String = "src/test/resources"

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return camelOrWordsToSnake(camelCase)
    }
}