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