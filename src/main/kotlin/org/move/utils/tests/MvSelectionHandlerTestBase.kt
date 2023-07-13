/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

import com.intellij.codeInsight.editorActions.SelectWordHandler
import com.intellij.ide.DataManager
import org.intellij.lang.annotations.Language

abstract class MvSelectionHandlerTestBase : MvTestBase() {
    fun doTest(@Language("Move") before: String,  @Language("Move") vararg after: String) {
        doTestInner(before, after.toList())
    }

    private fun doTestInner(before: String, after: List<String>) {
        myFixture.configureByText("main.move", before)
        val action = SelectWordHandler(null)
        val dataContext = DataManager.getInstance().getDataContext(myFixture.editor.component)
        for (text in after) {
            action.execute(myFixture.editor, null, dataContext)
            myFixture.checkResult(text, false)
        }
    }

    fun doTestWithTrimmedMargins(before: String, vararg after: String) {
        doTest(before.trimMargin(), *after.map { it.trimMargin() }.toTypedArray())
    }
}
