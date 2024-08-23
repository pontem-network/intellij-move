package org.move.ide

import com.intellij.testFramework.UsefulTestCase
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase

class MvBreadcrumbsProviderTest : MvTestBase() {
    fun `test breadcrumbs`() = doTextTest(
        """
        module 0x1::M {
            fun main() {
                while (true) if (true) /*caret*/;
            }
        }
    """, """
        0x1::M
        main()
        {...}
        while (true)
        if (true)
    """
    )

    fun `test breadcrumbs in module spec`() = doTextTest(
        """
        module 0x1::m {
            fun main() {}
        }
        spec 0x1::m {
            spec main {
                /*caret*/
            }
        }
    """, """
        spec: 0x1::m
        spec: main
    """
    )

    fun `test breadcrumbs for expr`() = doTextTest(
        """
        module 0x1::m {
            enum S { One, Two }
            fun main() {
                let s: S;
                for (i in 0..10) {
                    /*caret*/
                }
            }
        }
    """, """
        0x1::m
        main()
        {...}
        for(i in 0..10)
        {...}
    """
    )

    private fun doTextTest(@Language("Move") content: String, info: String) {
        InlineFile(content.trimIndent())
        val crumbs = myFixture.breadcrumbsAtCaret.joinToString(separator = "\n") { it.text }
        UsefulTestCase.assertSameLines(info.trimIndent(), crumbs)
    }
}
