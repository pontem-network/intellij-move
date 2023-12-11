package org.move.ide.notifications

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.settings.PerProjectMoveConfigurable
import org.move.cli.settings.aptosExec
import org.move.cli.settings.aptosPath
import org.move.cli.settings.isValidExecutable
import org.move.lang.isMoveFile
import org.move.lang.isMoveTomlManifestFile
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.showSettings

class InvalidAptosExecNotification(project: Project): MvEditorNotificationProvider(project),
                                                      DumbAware {

    override val VirtualFile.disablingKey: String get() = NOTIFICATION_STATUS_KEY + path

    override fun createNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {
        if (isUnitTestMode) return null
        if (!(file.isMoveFile || file.isMoveTomlManifestFile)) return null
        @Suppress("UnstableApiUsage")
        if (!project.isTrusted()) return null
        if (isNotificationDisabled(file)) return null

        if (project.aptosExec.isValid()) return null
//        if (project.aptosPath.isValidExecutable()) return null

        return EditorNotificationPanel().apply {
            text = "Aptos binary path is not provided or invalid"
            createActionLabel("Configure") {
                project.showSettings<PerProjectMoveConfigurable>()
            }
            createActionLabel("Do not show again") {
                disableNotification(file)
                updateAllNotifications(project)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_STATUS_KEY = "org.move.hideMoveNotifications"
    }
}
