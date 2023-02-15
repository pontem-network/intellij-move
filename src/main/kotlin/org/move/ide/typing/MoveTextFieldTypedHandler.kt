package org.move.ide.typing

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionPhase.EmptyAutoPopup
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.move.lang.core.psi.MvCodeFragment

class MoveTextFieldTypedHandler : TypedHandlerDelegate() {
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is MvCodeFragment) return Result.CONTINUE

        val phase = CompletionServiceImpl.getCompletionPhase()
        if (charTyped == ':') {
            if (phase is EmptyAutoPopup && phase.allowsSkippingNewAutoPopup(editor, charTyped)) {
                return Result.CONTINUE
            }

            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
            return Result.STOP
        }
        return Result.CONTINUE
    }
}
