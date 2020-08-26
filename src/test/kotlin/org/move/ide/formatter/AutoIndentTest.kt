package org.move.ide.formatter

import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveTestCase
import org.move.utils.tests.MoveTypingTestCase

class AutoIndentTest : MoveTypingTestCase() {
    fun `test script`() = doTestByText("""
        script {/*caret*/}
    """, """
        script {
            /*caret*/
        }
    """)

    fun `test module`() = doTestByText("""
        module M {/*caret*/}
    """, """
        module M {
            /*caret*/
        }
    """)

    fun `test address`() = doTestByText("""
        address 0x0 {/*caret*/}
    """, """
        address 0x0 {
            /*caret*/
        }
    """)

    fun `test function`() = doTestByText("""
        script {
            fun main() {/*caret*/}
        }
    """, """
        script {
            fun main() {
                /*caret*/
            }
        }
    """)

    fun `test second function in module`() = doTestByText("""
       module M {
           fun main() {}/*caret*/ 
       }
    """, """
       module M {
           fun main() {}
           /*caret*/
       }
    """)
}