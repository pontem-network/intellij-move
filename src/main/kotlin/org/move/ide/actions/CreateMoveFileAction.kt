package org.move.ide.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.util.IncorrectOperationException
import com.jetbrains.rd.util.firstOrNull
import org.apache.velocity.runtime.parser.ParseException
import org.move.ide.MoveIcons
import org.move.lang.moveProject
import java.util.*

class CreateMoveFileAction : CreateFileFromTemplateAction(CAPTION, "", MoveIcons.MOVE),
                             DumbAware {
    override fun getActionName(directory: PsiDirectory, newName: String, templateName: String) = CAPTION

    override fun buildDialog(
        project: Project,
        directory: PsiDirectory,
        builder: CreateFileFromTemplateDialog.Builder
    ) {
        builder.setTitle(CAPTION)
            .addKind("Empty", MoveIcons.MOVE, "Move File")
            .addKind("Module", MoveIcons.MOVE, "Move Module")
            .addKind("Test", MoveIcons.MOVE, "Move Test Module")
    }

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
        return createFileFromTemplate(dir, name, template)
    }

    // TODO: it's possible to create a dialog asking for ADDRESS before creating a file,
    // see https://youtrack.jetbrains.com/issue/IDEA-161451
    fun createFileFromTemplate(
        dir: PsiDirectory,
        fileName: String,
        template: FileTemplate,
    ): PsiFile? {
        val project = dir.project
        val properties = Properties(FileTemplateManager.getInstance(dir.project).defaultProperties)
        properties.setProperty("ADDRESS", getFirstNamedAddress(dir))

        try {
            val psiFile = FileTemplateUtil.createFromTemplate(
                template,
                fileName,
                properties,
                dir
            )
                .containingFile
            val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiFile)
            val virtualFile = psiFile.virtualFile
            if (virtualFile != null) {
                FileEditorManager.getInstance(project).openFile(virtualFile, true)
                return pointer.element
            }
        } catch (e: ParseException) {
            throw IncorrectOperationException("Error parsing Velocity template: " + e.message, e as Throwable)
        } catch (e: IncorrectOperationException) {
            throw e
        } catch (e: java.lang.Exception) {
            LOG.error(e)
        }

        return null
    }

    private fun getFirstNamedAddress(dir: PsiDirectory): String {
        return dir.moveProject?.currentPackageAddresses()?.firstOrNull()?.key ?: DEFAULT_ADDRESS
    }

    private companion object {
        private const val CAPTION = "Move File"
        private const val DEFAULT_ADDRESS = "0x1"
    }
}
