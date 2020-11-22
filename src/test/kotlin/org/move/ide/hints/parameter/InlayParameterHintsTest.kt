package org.move.ide.hints.parameter

import com.intellij.codeInsight.daemon.impl.HintRenderer
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MoveTestCase

class InlayParameterHintsTest : MoveTestCase() {
    fun `test fun`() = checkByText("""
        module M {
            fun call(val: u8, val2: u8) {}
            fun main() {
                call(/*hint="val:"*/1, /*hint="val2:"*/2)
            }    
        }    
    """)

    fun `test too many arguments`() = checkByText("""
        module M {
            fun call(val: u8) {}
            fun main() {
                call(/*hint="val:"*/1, 2)
            }    
        }    
    """)

    fun `test no hint for first argument of assert`() = checkByText("""
        module M {
            fun main() {
                assert(true, /*hint="err:"*/2)
            }    
        }    
    """)

    fun `test no hint if variable name is the same as parameter name`() = checkByText("""
        module M {
            fun call(account: address) {}
            fun main() {
                let account = 0x1;
                call(account)
            }    
        }    
    """)

    private fun checkByText(@Language("Move") code: String) {
        InlineFile(code.replace(
            HINT_COMMENT_PATTERN,
            "<$1/>"
        ))
        myFixture.testInlays({ (it.renderer as HintRenderer).text }) { it.renderer is HintRenderer }
    }

    companion object {
        private val HINT_COMMENT_PATTERN = Regex("""/\*(hint.*?)\*/""")
    }

}