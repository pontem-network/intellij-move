package org.move.ide.refactoring

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveTestCase

class RenameTest : MoveTestCase() {
    fun `test function argument`() = doTest("spam", """
        script {
            fun main(/*caret*/account: &signer) {
                account;
            }
        }
    """, """
        script {
            fun main(spam: &signer) {
                spam;
            }
        }
    """)

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

    fun `test local variable shadowed`() = doTest("spam", """
        script {
            fun main() {
                let /*caret*/a = 1;
                let a = a + 1;
                a;
            }
        }
    """, """
        script {
            fun main() {
                let spam = 1;
                let a = spam + 1;
                a;
            }
        }
    """)

    fun `test function name`() = doTest("renamed_call", """
        module M {
            fun /*caret*/call() {
                1
            }

            fun main() {
                call();
            }
        }
    """, """
        module M {
            fun renamed_call() {
                1
            }

            fun main() {
                renamed_call();
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
        myFixture.renameElement(element, newName, false, false)
        myFixture.checkResult(after)
    }
}