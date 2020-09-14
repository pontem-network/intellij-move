package org.move.openapiext

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.HighlighterIterator


fun isValidOffset(offset: Int, text: CharSequence): Boolean =
    0 <= offset && offset <= text.length


fun Editor.createLexer(offset: Int): HighlighterIterator? {
    if (!isValidOffset(offset, document.charsSequence)) return null
    val lexer = (this as EditorEx).highlighter.createIterator(offset)
    if (lexer.atEnd()) return null
    return lexer
}
