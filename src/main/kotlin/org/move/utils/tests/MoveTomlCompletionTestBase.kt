package org.move.utils.tests

import org.intellij.lang.annotations.Language
import org.move.utils.tests.completion.MoveCompletionTestFixture

abstract class MoveTomlCompletionTestBase: MoveTestBase() {
    protected lateinit var completionFixture: MoveCompletionTestFixture

    override fun setUp() {
        super.setUp()
        completionFixture = MoveCompletionTestFixture(myFixture, "Move.toml")
        completionFixture.setUp()
    }

    override fun tearDown() {
        completionFixture.tearDown()
        super.tearDown()
    }

    protected fun doSingleCompletion(
        @Language("TOML") before: String,
        @Language("TOML") after: String
    ) = completionFixture.doSingleCompletion(before, after)

//    protected fun doSingleCompletionByFileTree(
//        fileTree: FileTree,
//        @Language("TOML") after: String
//    ) = completionFixture.doSingleCompletionByFileTree(fileTree, after)

    protected fun checkNoCompletion(@Language("TOML") code: String) = completionFixture.checkNoCompletion(code)
}
