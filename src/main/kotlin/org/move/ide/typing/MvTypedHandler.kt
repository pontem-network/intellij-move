package org.move.ide.typing

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.move.lang.MoveFile
import org.move.lang.MvElementTypes.AT
import org.move.lang.MvElementTypes.COLON_COLON

class MvTypedHandler : TypedHandlerDelegate() {
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is MoveFile) return Result.CONTINUE

        val offset = editor.caretModel.offset

        if (charTyped == '@') {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC) { f ->
                val leaf = f.findElementAt(offset)
                leaf.elementType == AT
            }
            return Result.STOP
        }
        // `:` is typed right after `:`
        if (
            charTyped == ':'
            && StringUtil.endsWith(editor.document.immutableCharSequence, 0, offset, ":")
        ) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC) { f ->
                val leaf = f.findElementAt(offset - 1)
                leaf.elementType == COLON_COLON
            }
            return Result.STOP
        }

        return Result.CONTINUE
    }
}
