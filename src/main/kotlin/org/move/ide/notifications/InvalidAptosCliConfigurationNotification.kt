package org.move.ide.notifications

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.settings.PerProjectAptosConfigurable
import org.move.cli.settings.aptosCliPath
import org.move.cli.settings.isValidExecutable
import org.move.cli.settings.moveSettings
import org.move.openapiext.showSettingsDialog
import org.move.stdext.getCliFromPATH

class InvalidAptosCliConfigurationNotification(project: Project): MvAptosEditorNotificationProvider(project),
                                                                  DumbAware {

    override val notificationProviderId: String get() = NOTIFICATION_STATUS_KEY

    override fun createAptosNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {

        if (project.aptosCliPath.isValidExecutable()) return null

        return EditorNotificationPanel().apply {
            text = "Aptos CLI path is not provided or invalid"

            val aptosCliFromPATH = getCliFromPATH("aptos")?.toString()
            if (aptosCliFromPATH != null) {
                createActionLabel("Set to \"$aptosCliFromPATH\"") {
                    project.moveSettings.modify {
                        it.aptosPath = aptosCliFromPATH
                    }
                }
            }

            createActionLabel("Configure") {
                project.showSettingsDialog<PerProjectAptosConfigurable>()
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
