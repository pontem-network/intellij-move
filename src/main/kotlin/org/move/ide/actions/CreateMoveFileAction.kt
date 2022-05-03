package org.move.ide.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.move.ide.MoveIcons

class CreateMoveFileAction : CreateFileFromTemplateAction(CAPTION, "", MoveIcons.MOVE),
                             DumbAware {
    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?) = CAPTION

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder.setTitle(CAPTION)
            .addKind("Empty", MoveIcons.MOVE, "Move File")
            .addKind("Module", MoveIcons.MOVE, "Move Module")
            .addKind("Script", MoveIcons.MOVE, "Move Script")
            .addKind("Test", MoveIcons.MOVE, "Move Test Module")
    }

    private companion object {
        private const val CAPTION = "Move File"
    }
}
