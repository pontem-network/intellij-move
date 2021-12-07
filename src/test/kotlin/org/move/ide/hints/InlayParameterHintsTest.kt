package org.move.ide.hints

import com.intellij.codeInsight.daemon.impl.HintRenderer
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase

class InlayParameterHintsTest : MvTestBase() {
    fun `test fun`() = checkByText(
        """
        module M {
            fun call(val: u8, val2: u8) {}
            fun main() {
                call(/*hint="val:"*/1, /*hint="val2:"*/2)
            }    
        }    
    """
    )

    fun `test too many arguments`() = checkByText(
        """
        module M {
            fun call(val: u8) {}
            fun main() {
                call(/*hint="val:"*/1, 2)
            }    
        }    
    """
    )

    fun `test no hint for first argument of assert`() = checkByText(
        """
        module M {
            fun main() {
                assert(true, /*hint="err:"*/2)
            }    
        }    
    """
    )

    fun `test no hint if variable name is the same as parameter name or superset case insensitive`() =
        checkByText(
            """
        module M {
            fun call(account: address, param: address) {}
            fun main() {
                let account = 0x1;
                call(account, /*hint="param:"*/0x1);
                
                let account_wanted = 0x1;
                call(account_wanted, /*hint="param:"*/0x1);

                let ACCOUNT_WANTED = 0x1;
                call(ACCOUNT_WANTED, /*hint="param:"*/0x1);
            }    
        }    
    """
        )

    private fun checkByText(@Language("Move") code: String) {
        inlineFile(
            code.replace(
                HINT_COMMENT_PATTERN,
                "<$1/>"
            )
        )
        myFixture.testInlays({ (it.renderer as HintRenderer).text }) { it.renderer is HintRenderer }
    }

    companion object {
        private val HINT_COMMENT_PATTERN = Regex("""/\*(hint.*?)\*/""")
    }

}
