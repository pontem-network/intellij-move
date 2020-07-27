package org.move.utils.tests.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class CompletionTestCase: BasePlatformTestCase() {
    lateinit var completionFixture: CompletionTestFixture

    override fun setUp() {
        super.setUp()
        completionFixture = CompletionTestFixture(myFixture)
        completionFixture.setUp()
    }

    override fun tearDown() {
        completionFixture.tearDown()
        super.tearDown()
    }
}