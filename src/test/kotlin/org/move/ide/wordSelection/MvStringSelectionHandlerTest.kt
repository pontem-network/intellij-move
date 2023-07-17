package org.move.ide.wordSelection

import org.move.utils.tests.MvSelectionHandlerTestBase

class MvStringSelectionHandlerTest: MvSelectionHandlerTestBase() {
    fun `test byte string`() = doTest("""
        module 0x1::m {
            fun main() {
                b"hello, <caret>world";
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                b"hello, <selection><caret>world</selection>";
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                b"<selection>hello, <caret>world</selection>";
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                <selection>b"hello, <caret>world"</selection>;
            }
        }        
    """)

    fun `test hex string`() = doTest("""
        module 0x1::m {
            fun main() {
                x"afff<caret>aa";
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                x"<selection>afff<caret>aa</selection>";
            }
        }        
    """, """
        module 0x1::m {
            fun main() {
                <selection>x"afff<caret>aa"</selection>;
            }
        }        
    """)
}