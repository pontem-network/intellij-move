package org.move.ide.hints

import com.intellij.codeInsight.hints.LinearOrderInlayRenderer
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase

class InlayTypeHintsTest : MvTestBase() {
    fun `test no type hint for unknown type`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let a = unknown();
            }
        }
    """
    )

    fun `test uninferred vector type for unknown item type`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let a/*hint text="[:  vector<?>]" */ = vector[unknown()];
            }
        }
    """
    )

    fun `test vector uninferred type`() = checkByText(
        """
        module 0x1::m {
            fun main() {
                let a/*hint text="[:  vector<?>]" */ = vector[];
            }
        }
    """
    )

    fun `test no redundant hints`() = checkByText("""
        module 0x1::m {
            fun main() {
                let _ = 1;
                let _a = 1;
                let a = unknown_ref;
            }
        }
    """)

    fun `test let stmt without expr`() = checkByText("""
        module 0x1::m {
            fun main() {
                let a/*hint text="[:  integer]"*/;
                a = 1;
            }
        }
    """)

    fun `test does not show obvious struct lit expr type`() = checkByText("""
        module 0x1::m {
            struct S<CoinType> {}
            struct USDT {}
            fun main() {
                let a = S<USDT> {};
            }
        }
    """)

    private fun checkByText(@Language("Move") code: String) {
        inlineFile(
            code.trimIndent()
                .replace(HINT_COMMENT_PATTERN, "<$1/>")
        )
        checkInlays()
    }

    @Suppress("UnstableApiUsage")
    private fun checkInlays() {
        myFixture.testInlays(
            {
                (it.renderer as LinearOrderInlayRenderer<*>).toString()
            },
            {
                it.renderer is LinearOrderInlayRenderer<*>
            }
        )
    }

    companion object {
        private val HINT_COMMENT_PATTERN = Regex("""/\*(hint.*?)\*/""")
    }
}
