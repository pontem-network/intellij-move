package org.move.lang.lexer

import com.intellij.openapi.editor.Editor
import org.intellij.lang.annotations.Language
import org.move.openapiext.createLexer
import org.move.utils.tests.MvTestBase
import org.move.utils.tests.replaceCaretMarker

class RestartableLexerTest : MvTestBase() {
    fun `test lexer restart closing block comment`() =
        doTestLexerRestart(""" /* module */*caret*/ module M {} """, '/')

    fun `test lexer restart outer address`() =
        doTestLexerRestart(""" module M {} addres/*caret*/ 0x0 {} """, 's')

    fun `test lexer restart outer address with other address block`() =
        doTestLexerRestart(""" module M {} addres/*caret*/ 0x0 {} address 0x1 {}""", 's')

    fun `test lexer restart`() =
        doTestLexerRestart(""" module M { fun main() { le/*caret*/} } """, 't')

    fun `test lexer restart address`() =
        doTestLexerRestart(""" module M { fun main() { let a: addres/*caret*/} } """, 's')

    fun `test lexer restart inside spec`() =
        doTestLexerRestart(""" module M { spec module { asser/*caret*/ true } fun main() { assert (1 == 1, 0); } } """, 't')

    fun `test lexer restart inside spec 2`() =
        doTestLexerRestart(""" module M { spec module { assert true } fun main() { asser/*caret*/(1 == 1, 0); } } """, 't')

    private fun doTestLexerRestart(@Language("Move") originalText: String, char: Char) {
        val text = replaceCaretMarker(originalText)
        var editor = createEditor("main.move", text)
        myFixture.type(char)
        val restartedLexer = editor.createLexer(0)!!

        editor = createEditor("copy.move", editor.document.text)
        val fullLexer = editor.createLexer(0)!!

        while (!restartedLexer.atEnd() && !fullLexer.atEnd()) {
            assertEquals(fullLexer.tokenType, restartedLexer.tokenType)
            assertEquals(fullLexer.start, restartedLexer.start)
            restartedLexer.advance()
            fullLexer.advance()
        }
        assertTrue(restartedLexer.atEnd() && fullLexer.atEnd())
    }

    private fun createEditor(filename: String, text: String): Editor {
        val file = myFixture.configureByText(filename, text)
        myFixture.openFileInEditor(file.virtualFile)
        return myFixture.editor
    }
}
