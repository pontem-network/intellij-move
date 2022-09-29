package org.move.ide.typing

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.lang.MoveFile
import org.move.lang.MvElementTypes.*
import org.move.lang.core.tokenSetOf
import org.move.openapiext.isValidOffset

private val INVALID_INSIDE_TOKENS = tokenSetOf(L_BRACE, R_BRACE, SEMICOLON)

class MvAngleBraceTypedHandler : TypedHandlerDelegate() {
    private var ltTyped = false

    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType
    ): Result {
        if (file !is MoveFile) return Result.CONTINUE

        if (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
            when (c) {
                '<' -> ltTyped = isStartOfGenericBraces(editor)
                '>' -> {
                    val lexer = editor.createLexer(editor.caretModel.offset) ?: return Result.CONTINUE
                    val tokenType = lexer.tokenType
                    if (tokenType == GT && calculateBalance(editor) == 0) {
                        EditorModificationUtil.moveCaretRelatively(editor, 1)
                        return Result.STOP
                    }
                }
            }
        }

        return Result.CONTINUE
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is MoveFile) return Result.CONTINUE

        if (ltTyped) {
            ltTyped = false
            val balance = calculateBalance(editor)
            if (balance == 1) {
                val offset = editor.caretModel.offset
                editor.document.insertString(offset, ">")
            }
        }

        return super.charTyped(c, project, editor, file)
    }
}

class MvAngleBracketsBackspaceHandler : BackspaceHandlerDelegate() {
    private var enabled: Boolean = false

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        enabled = deleting(c, file, editor)
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        if (!enabled) return false
        return deleted(editor)
    }

    /**
     * Determine whether this handler applies to given context and perform necessary actions before deleting [c].
     */
    fun deleting(c: Char, file: PsiFile, editor: Editor): Boolean {
        if (c == '<' && file is MoveFile) {
            val offset = editor.caretModel.offset
            val iterator = (editor as EditorEx).highlighter.createIterator(offset)
            return iterator.tokenType == GT
        }
        return false
    }

    /**
     * Perform action after char [c] was deleted.
     *
     * @return true whether this handler succeeded and the IDE should stop evaluating
     *         remaining handlers; otherwise, false
     */
    fun deleted(editor: Editor): Boolean {
        val balance = calculateBalance(editor)
        if (balance < 0) {
            val offset = editor.caretModel.offset
            editor.document.deleteString(offset, offset + 1)
            return true
        }
        return true
    }
}

private fun isStartOfGenericBraces(editor: Editor): Boolean {
    val offset = editor.caretModel.offset
    val lexer = editor.createLexer(offset - 1) ?: return false
    if (lexer.tokenType != IDENTIFIER) return false

    // don't complete angle braces inside identifier
    if (lexer.end != offset) return false
    return true
}

private fun calculateBalance(editor: Editor): Int {
    val offset = editor.caretModel.offset
    val iterator = (editor as EditorEx).highlighter.createIterator(offset)
    while (iterator.start > 0 && iterator.tokenType !in INVALID_INSIDE_TOKENS) {
        iterator.retreat()
    }

    if (iterator.tokenType in INVALID_INSIDE_TOKENS) {
        iterator.advance()
    }

    var balance = 0
    while (!iterator.atEnd() && balance >= 0 && iterator.tokenType !in INVALID_INSIDE_TOKENS) {
        when (iterator.tokenType) {
            LT -> balance++
            GT -> balance--
        }
        iterator.advance()
    }

    return balance
}

fun Editor.createLexer(offset: Int): HighlighterIterator? {
    if (!isValidOffset(offset, document.charsSequence)) return null
    val lexer = (this as EditorEx).highlighter.createIterator(offset)
    if (lexer.atEnd()) return null
    return lexer
}
