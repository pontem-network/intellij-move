package org.move.ide.actions

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.actions.AttributesDefaults
import com.intellij.ide.fileTemplates.ui.CreateFromTemplateDialog
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.jetbrains.rd.util.firstOrNull
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
            .addKind("Script", MoveIcons.MOVE, "Move Script")
            .addKind("Test", MoveIcons.MOVE, "Move Test Module")
    }

    override fun createFileFromTemplate(name: String, template: FileTemplate, dir: PsiDirectory): PsiFile? {
        val moduleName = name.removeSuffix(".move")
        return createFileFromTemplate(dir, moduleName, template)
    }

    fun createFileFromTemplate(dir: PsiDirectory, moduleName: String, template: FileTemplate): PsiFile? {
        val project = dir.project
        val defaultProperties = FileTemplateManager.getInstance(project).defaultProperties

        val properties = Properties(defaultProperties)
        properties.setProperty("ADDRESS", getDefaultAddressForDirectory(dir))

        val element = try {
            CreateFromTemplateDialog(
                project, dir, template,
                AttributesDefaults(moduleName).withFixedName(true),
                properties
            ).create()
        } catch (e: IncorrectOperationException) {
            throw e
        } catch (e: Exception) {
            LOG.error(e)
            return null
        }

        return element?.containingFile
    }

    private fun getDefaultAddressForDirectory(dir: PsiDirectory): String {
        return dir.moveProject?.packageAddresses()?.firstOrNull()?.key
            ?: DEFAULT_ADDRESS
    }

    private companion object {
        private const val CAPTION = "Move File"
        private const val DEFAULT_ADDRESS = "0x11"
    }
}
