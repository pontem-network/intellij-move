package org.move.utils.tests.parser

import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.annotations.NonNls
import org.move.cli.settings.MvProjectSettingsService
import org.move.lang.MoveParserDefinition
import org.move.utils.tests.base.TestCase
import org.move.utils.tests.handleCompilerV2Annotations


abstract class MvParsingTestCase(@NonNls dataPath: String): ParsingTestCase(
    "org/move/lang/parser/$dataPath",
    "move",
    true,
    MoveParserDefinition()
) {
    override fun setUp() {
        super.setUp()

        project.registerService(MvProjectSettingsService::class.java)

        this.handleCompilerV2Annotations(project)
    }

    override fun getTestDataPath(): String = "src/test/resources"

    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }
}
