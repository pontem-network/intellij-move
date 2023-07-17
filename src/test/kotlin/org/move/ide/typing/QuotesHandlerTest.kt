package org.move.ide.typing

import org.move.utils.tests.MvTypingTestCase

class QuotesHandlerTest: MvTypingTestCase() {
    fun `test no auto insertion if no prefix`() = doTestByText("""
    script {
        fun m() {
            /*caret*/;
        }
    }    
    """, """
    script {
        fun m() {
            "/*caret*/;
        }
    }    
    """, '"')

    fun `test complete byte string quotes no semi`() = doTestByText("""
    script {
        fun m() {
            b<caret>
        }
    }    
    """, """
    script {
        fun m() {
            b"<caret>"
        }
    }    
    """, '"')

    fun `test complete hex string quotes semi`() = doTestByText("""
    script {
        fun m() {
            x/*caret*/;
        }
    }    
    """, """
    script {
        fun m() {
            x"/*caret*/";
        }
    }    
    """, '"')

    fun `test complete hex string quotes no semi`() = doTestByText("""
    script {
        fun m() {
            x/*caret*/
        }
    }    
    """, """
    script {
        fun m() {
            x"/*caret*/"
        }
    }    
    """, '"')
}
