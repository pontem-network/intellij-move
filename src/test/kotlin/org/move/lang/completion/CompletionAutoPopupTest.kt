package org.move.lang.completion

import com.intellij.testFramework.fixtures.CompletionAutoPopupTester
import com.intellij.util.ThrowableRunnable
import org.intellij.lang.annotations.Language
import org.move.utils.tests.NamedAddress
import org.move.utils.tests.completion.CompletionTestCase

class CompletionAutoPopupTest: CompletionTestCase() {
    private lateinit var tester: CompletionAutoPopupTester

    fun `test popup is not shown when typing variable name`() = checkPopupIsNotShownAfterTyping(
        """
        module 0x1::m {
            struct MyStruct { val: u8 }
            fun main() {
                let tr/*caret*/
            }
        }        
    """, "u"
    )

    fun `test popup is shown when typing name starting with upper case`() = checkPopupIsShownAfterTyping(
        """
        module 0x1::m {
            struct MyStruct { val: u8 }
            fun main() {
                let Str/*caret*/
            }
        }        
    """, "u"
    )

    fun `test popup is shown for struct pat fields`() = checkPopupIsShownAfterTyping(
        """
        module 0x1::m {
            struct MyStruct { val: u8 }
            fun main() {
                let MyStruct { v/*caret*/ };
            }
        }        
    """, "a"
    )

    override fun setUp() {
        super.setUp()
        tester = CompletionAutoPopupTester(myFixture)
    }

    override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
        tester.runWithAutoPopupEnabled(testRunnable)
    }

    override fun runInDispatchThread(): Boolean = false

    private fun checkPopupIsShownAfterTyping(@Language("Move") code: String, toType: String) {
        configureAndType(code, toType)
        assertNotNull(tester.lookup)
    }

    private fun checkPopupIsNotShownAfterTyping(@Language("Move") code: String, toType: String) {
        configureAndType(code, toType)
        assertNull(tester.lookup)
    }

    private fun configureAndType(code: String, toType: String) {
        InlineFile(code).withCaret()
        tester.typeWithPauses(toType)
    }
}