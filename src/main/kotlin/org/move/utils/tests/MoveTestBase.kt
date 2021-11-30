/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

import com.intellij.openapi.util.io.StreamUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language
import org.move.utils.tests.base.MoveTestCase
import org.move.utils.tests.base.TestCase

abstract class MoveTestBase : BasePlatformTestCase(),
                              MoveTestCase {
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

    companion object {
        @JvmStatic
        fun getResourceAsString(path: String): String? {
            val stream = MoveTestBase::class.java.classLoader.getResourceAsStream(path)
                ?: return null
            return StreamUtil.readText(stream, Charsets.UTF_8)
        }
    }
}
