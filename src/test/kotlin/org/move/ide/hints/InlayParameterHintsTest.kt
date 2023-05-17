package org.move.ide.hints

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.codeInsight.hints.LinearOrderInlayRenderer
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase

class InlayParameterHintsTest : MvTestBase() {
    fun `test fun`() = checkByText(
        """
        module 0x1::M {
            fun call(val: u8, val2: u8) {}
            fun main() {
                call(/*hint="val:"*/1, /*hint="val2:"*/2)
            }    
        }    
    """
    )

    fun `test fun skipped argument`() = checkByText(
        """
        module 0x1::M {
            fun call(val: u8, val2: u8) {}
            fun main() {
                call(, /*hint="val2:"*/2)
            }    
        }    
    """
    )

    fun `test too many arguments`() = checkByText(
        """
        module 0x1::M {
            fun call(val: u8) {}
            fun main() {
                call(/*hint="val:"*/1, 2)
            }    
        }    
    """
    )

    fun `test no hint for first argument of assert`() = checkByText(
        """
        module 0x1::M {
            fun main() {
                assert(true, /*hint="err:"*/2)
            }    
        }    
    """
    )

    fun `test no hint if reference`() =
        checkByText(
            """
        module 0x1::M {
            fun call(root_acc: address, param: address) {}
            fun main() {
                let myval<hint text="[:  address]"/> = @0x1;
                call(myval, /*hint="param:"*/@0x1);
            }    
        }    
    """
        )

    private fun checkByText(@Language("Move") code: String) {
        inlineFile(
            code.trimIndent().replace(HINT_COMMENT_PATTERN, "<$1/>")
        )
        checkInlays()
    }

    @Suppress("UnstableApiUsage")
    private fun checkInlays() {
        myFixture.testInlays(
            { (it.renderer as LinearOrderInlayRenderer<*>).toString() },
            { it.renderer is LinearOrderInlayRenderer<*> }
        )
    }

    companion object {
        private val HINT_COMMENT_PATTERN = Regex("""/\*(hint.*?)\*/""")
    }

}
