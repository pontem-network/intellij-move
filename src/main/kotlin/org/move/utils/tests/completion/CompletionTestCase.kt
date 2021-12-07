package org.move.utils.tests.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language

abstract class CompletionTestCase : BasePlatformTestCase() {
    lateinit var completionFixture: MoveCompletionTestFixture

    override fun setUp() {
        super.setUp()
        completionFixture = MoveCompletionTestFixture(myFixture)
        completionFixture.setUp()
    }

    override fun tearDown() {
        completionFixture.tearDown()
        super.tearDown()
    }

    protected fun doFirstCompletion(
        @Language("Move") before: String,
        @Language("Move") after: String
    ) = completionFixture.doFirstCompletion(before, after)

    protected fun doSingleCompletion(
        @Language("Move") before: String,
        @Language("Move") after: String
    ) = completionFixture.doSingleCompletion(before, after)

    protected fun checkContainsCompletion(
        variant: String,
        @Language("Move") code: String
    ) = completionFixture.checkContainsCompletion(code, variant)

    protected fun checkCompletion(
        lookupString: String,
        @Language("Move") before: String,
        @Language("Move") after: String,
        completionChar: Char = '\n',
    ) = completionFixture.checkCompletion(lookupString, before, after, completionChar)

    protected fun checkNotContainsCompletion(
        variant: String,
        @Language("Move") code: String
    ) = completionFixture.checkNotContainsCompletion(code, variant)

    protected fun checkNoCompletion(@Language("Move") code: String) = completionFixture.checkNoCompletion(code)

}
