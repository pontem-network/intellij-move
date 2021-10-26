package org.move.utils.tests

import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import org.move.utils.tests.base.TestCase

abstract class MoveHeavyTestBase : CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {
    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }
}
