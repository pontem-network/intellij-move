package org.move.ide.liveTemplates

import com.intellij.openapi.actionSystem.IdeActions
import org.intellij.lang.annotations.Language
import org.move.utils.tests.MvTestBase

class MvLiveTemplatesTest: MvTestBase() {

    fun `test no test function in type`() = noSnippet("""
        module 0x1::m {
            struct S { val: t/*caret*/}
        }        
    """)

    fun `test no test function in struct fields block`() = noSnippet("""
        module 0x1::m {
            struct S { t/*caret*/ }
        }        
    """)

    fun `test no test function in enum variants block`() = noSnippet("""
        module 0x1::m {
            enum S { t/*caret*/ }
        }        
    """)

    fun `test no test function in tuple struct type`() = noSnippet("""
        module 0x1::m {
            struct S(u8, t/*caret*/)
        }        
    """)

    fun `test no test function in expr`() = noSnippet("""
        module 0x1::m {
            fun main() {
                t/*caret*/
            }
        }        
    """)

    fun `test no test function outside module`() = noSnippet("""
        t/*caret*/
        module 0x1::m {
        }        
    """)

    fun `test test function in module body`() = expandSnippet("""
        module 0x1::m {
            t/*caret*/
        }        
    """, """
        module 0x1::m {
            #[test]
            fun /*caret*/() {
                
            }
        }        
    """)

    private fun expandSnippet(@Language("Move") before: String, @Language("Move") after: String) =
        checkEditorAction(before, after, IdeActions.ACTION_EXPAND_LIVE_TEMPLATE_BY_TAB)

    private fun noSnippet(@Language("Move") code: String) = expandSnippet(code, code)
}