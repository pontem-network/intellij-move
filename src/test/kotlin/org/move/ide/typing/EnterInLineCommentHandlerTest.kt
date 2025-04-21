package org.move.ide.typing

import org.move.utils.tests.MvTypingTestCase

class EnterInLineCommentHandlerTest: MvTypingTestCase() {
    override val dataPath = "org/move/ide/typing/lineComment.fixtures"

    fun `test in outer doc comment`() = doTestByText("""
    /// multi<caret>ply by two
    module 0x1::M {}    
    """, """
    /// multi
    /// <caret>ply by two
    module 0x1::M {}    
    """)

    fun `test after outer doc comment`() = doTestByText("""
/// multiply by two<caret>
module 0x1::M {}
    """, """
/// multiply by two
/// <caret>
module 0x1::M {}
    """)
}
