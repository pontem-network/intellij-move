package org.move.ide.refactoring

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveTestCase

class RenameTest : MoveTestCase() {
    fun `test local variable`() = doTest("spam", """
        script {
            fun main() {
                let /*caret*/a = 1;
                a;
            }
        }
    """, """
        script {
            fun main() {
                let spam = 1;
                spam;
            }
        }
    """)

    private fun doTest(
        newName: String,
        @Language("Move") before: String,
        @Language("Move") after: String
    ) {
        InlineFile(before).withCaret()
        val element = myFixture.elementAtCaret
        myFixture.renameElement(element, newName, true, true)
        myFixture.checkResult(after)
    }
}