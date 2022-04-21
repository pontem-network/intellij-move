/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language
import org.move.utils.tests.base.MvTestCase
import org.move.utils.tests.base.TestCase

abstract class MvTestBase : BasePlatformTestCase(),
                            MvTestCase {
    protected val fileName: String
        get() = "${getTestName(true)}.$testFileExtension"
    open val dataPath: String = ""

    override fun getTestDataPath(): String = "${TestCase.testResourcesPath}/$dataPath"
    override fun getTestName(lowercaseFirstLetter: Boolean): String {
        val camelCase = super.getTestName(lowercaseFirstLetter)
        return TestCase.camelOrWordsToSnake(camelCase)
    }

    protected fun inlineFile(@Language("Move") code: String, name: String = "main.move"): InlineFile {
        return InlineFile(myFixture, code, name)
    }

    protected fun checkByText(
        @Language("Move") before: String,
        @Language("Move") after: String,
        action: () -> Unit,
    ) {
        inlineFile(before)
        action()
        myFixture.checkResult(replaceCaretMarker(after))
    }

    protected fun checkByFile(ignoreTrailingWhitespace: Boolean = true, action: () -> Unit) {
        val (before, after) = (fileName to fileName.replace(".move", "_after.move"))
        myFixture.configureByFile(before)
        action()
        myFixture.checkResultByFile(after, ignoreTrailingWhitespace)
    }

    protected fun FileTree.prepareTestProjectFromFixture(): TestProject = prepareTestProject(myFixture)
    protected fun FileTree.createAndOpenFileWithCaretMarker(): TestProject =
        createAndOpenFileWithCaretMarker(myFixture)

    protected open fun checkEditorAction(
        @Language("Rust") before: String,
        @Language("Rust") after: String,
        actionId: String,
        trimIndent: Boolean = true,
    ) {
        fun String.trimIndentIfNeeded(): String = if (trimIndent) trimIndent() else this

        checkByText(before.trimIndentIfNeeded(), after.trimIndentIfNeeded()) {
            myFixture.performEditorAction(actionId)
        }
    }
}
