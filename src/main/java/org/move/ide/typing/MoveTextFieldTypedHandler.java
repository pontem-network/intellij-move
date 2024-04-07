package org.move.ide.typing;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionPhase;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.move.lang.core.psi.MvCodeFragment;

public class MoveTextFieldTypedHandler extends TypedHandlerDelegate {
    @NotNull
    @Override
    public Result checkAutoPopup(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (!(file instanceof MvCodeFragment)) return Result.CONTINUE;

        var phase = CompletionServiceImpl.getCompletionPhase();
        if (charTyped == ':') {
            if (phase instanceof CompletionPhase.EmptyAutoPopup
                    && ((CompletionPhase.EmptyAutoPopup) phase).allowsSkippingNewAutoPopup(editor, charTyped)) {
                return Result.CONTINUE;
            }

            AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
            return Result.STOP;
        }
        return Result.CONTINUE;
    }
}
