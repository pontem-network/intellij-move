package org.move.ide.notifications

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.runConfigurations.aptos.AptosTool
import org.move.cli.update.PeriodicCheckForUpdatesService

class UpdateAvailableNotification(project: Project): MvAptosEditorNotificationProvider(project) {
    override val notificationProviderId: String
        get() = "org.move.UpdateAvailableNotification"

    override fun createAptosNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {

        if (!PeriodicCheckForUpdatesService.isEnabled) return null

        val updateService = project.service<PeriodicCheckForUpdatesService>()

        return EditorNotificationPanel().apply {
            createActionLabel("Update aptos") {
                updateService.doToolUpdate(AptosTool.APTOS)
            }
            createActionLabel("Update movefmt") {
                updateService.doToolUpdate(AptosTool.MOVEFMT)
            }
            `Do not show again`(file)
        }
    }
}