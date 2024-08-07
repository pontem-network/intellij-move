package org.move.bytecode

import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import org.move.bytecode.AptosBytecodeNotificationProvider.DecompilationModalTask
import org.move.cli.settings.getAptosCli
import org.move.ide.MoveIcons
import org.move.ide.notifications.showBalloon
import org.move.openapiext.openFile
import org.move.stdext.unwrapOrElse
import java.util.*

class DecompileAptosMvFileAction: DumbAwareAction("Decompile .mv File", null, MoveIcons.APTOS_LOGO) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE)?.virtualFile ?: return
        val decompilationTask = DecompilationModalTask.forVirtualFile(project, file)
            ?: run {
                project.showBalloon("Error with decompilation process", "Aptos CLI is not configured", ERROR)
                return
            }
        val decompiledFile = decompilationTask.runWithProgress()
            .unwrapOrElse {
                project.showBalloon("Error with decompilation process", it, ERROR)
                return
            }
        project.openFile(decompiledFile)
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val presentation = e.presentation
        val enabled =
            (file != null
                    && Objects.nonNull(file.virtualFile) && !(file.virtualFile.fileSystem.isReadOnly)
                    && file.fileType == AptosBytecodeFileType
                    && e.getData(CommonDataKeys.PROJECT)?.getAptosCli() != null)
        presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}