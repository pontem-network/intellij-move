package org.move.ide.notifications

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.settings.PerProjectEndlessConfigurable
import org.move.cli.settings.endlessCliPath
import org.move.cli.settings.isValidExecutable
import org.move.cli.settings.moveSettings
import org.move.openapiext.showSettingsDialog
import org.move.stdext.getCliFromPATH

class InvalidEndlessCliConfigurationNotification(project: Project): MvEndlessEditorNotificationProvider(project),
                                                                  DumbAware {

    override val notificationProviderId: String get() = NOTIFICATION_STATUS_KEY

    override fun createEndlessNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {

        if (project.endlessCliPath.isValidExecutable()) return null

        return EditorNotificationPanel().apply {
            text = "Endless CLI path is not provided or invalid"

            val endlessCliFromPATH = getCliFromPATH("endless")?.toString()
            if (endlessCliFromPATH != null) {
                createActionLabel("Set to \"$endlessCliFromPATH\"") {
                    project.moveSettings.modify {
                        it.endlessPath = endlessCliFromPATH
                    }
                }
            }

            createActionLabel("Configure") {
                project.showSettingsDialog<PerProjectEndlessConfigurable>()
            }
            createActionLabel("Do not show again") {
                disableNotification(file)
                updateAllNotifications(project)
            }
        }
    }

    override val enableForScratchFiles: Boolean get() = true

    override val enableForDecompiledFiles: Boolean get() = true

    companion object {
        private const val NOTIFICATION_STATUS_KEY = "org.move.hideMoveNotifications"
    }
}
