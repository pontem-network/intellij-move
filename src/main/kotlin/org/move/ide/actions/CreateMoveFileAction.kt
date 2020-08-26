package org.move.ide.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.move.ide.MvIcons

class CreateMoveFileAction : CreateFileFromTemplateAction(CAPTION, "", MvIcons.MOVE), DumbAware {
    override fun getActionName(directory: PsiDirectory?, newName: String, templateName: String?): String =
        CAPTION

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder.setTitle(CAPTION).addKind("Empty file", MvIcons.MOVE, "Move File")
    }

    private companion object {
        private const val CAPTION = "Move File"
    }
}