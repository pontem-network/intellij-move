/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.utils.tests

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionDelegate
import com.intellij.util.ui.UIUtil
import org.intellij.lang.annotations.Language
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class MoveIntentionTestCase(private val intentionClass: KClass<out IntentionAction>) :
    MoveTestBase() {

    protected val intention: IntentionAction
        get() = findIntention() ?: error("Failed to find `${intentionClass.simpleName}` intention")

//    fun `test intention has documentation`() {
//        if (!intentionClass.isSubclassOf(MoveElementBaseIntentionAction::class)) return
//
//        val directory = "intentionDescriptions/${intentionClass.simpleName}"
//        val description = checkFileExists(Paths.get(directory, "description.html"))
//        checkHtmlStyle(description)
//
//        checkFileExists(Paths.get(directory, "before.rs.template"))
//        checkFileExists(Paths.get(directory, "after.rs.template"))
//    }

    private fun checkFileExists(path: Path): String = getResourceAsString(path.toString())
        ?: error("No ${path.fileName} found for $intentionClass ($path)")

    protected fun doAvailableTest(
        @Language("Move") before: String,
        @Language("Move") after: String,
        fileName: String = "main.move"
    ) {
        InlineFile(myFixture, before.trimIndent(), fileName).withCaret()
        launchAction()
        myFixture.checkResult(replaceCaretMarker(after.trimIndent()))
    }

    @Suppress("unused")
    protected fun doAvailableTestWithFileTree(
        @Language("Move") fileStructureBefore: String,
        @Language("Move") openedFileAfter: String
    ) {
        fileTreeFromText(fileStructureBefore).createAndOpenFileWithCaretMarker()
        launchAction()
        myFixture.checkResult(replaceCaretMarker(openedFileAfter.trimIndent()))
    }

    protected fun launchAction() {
        UIUtil.dispatchAllInvocationEvents()
        myFixture.launchAction(intention)
    }

    protected fun doUnavailableTest(@Language("Move") before: String, fileName: String = "main.move") {
        InlineFile(myFixture, before, fileName).withCaret()
        val intention = findIntention()
        check(intention == null) {
            "\"${intentionClass.simpleName}\" should not be available"
        }
    }

    private fun findIntention(): IntentionAction? {
        return myFixture.availableIntentions.firstOrNull {
            val originalIntention = IntentionActionDelegate.unwrap(it)
            intentionClass == originalIntention::class
        }
    }
}
