package org.move.ide.notifications

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.update.AptosTool
import org.move.cli.update.PeriodicCheckForUpdatesService
import org.move.cli.update.toolUpdateService
import org.move.stdext.capitalized

abstract class MvToolUpdateAvailableNotificationBase(project: Project): MvAptosEditorNotificationProvider(project) {
    abstract val tool: AptosTool

    override val notificationProviderId: String
        get() = "org.move.${tool.id.capitalized()}UpdateAvailableNotification"

    override fun createAptosNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {
        if (!PeriodicCheckForUpdatesService.isEnabled) return null

        val toolUpdateService = project.toolUpdateService
        val newVersionAvailable = when (tool) {
            AptosTool.APTOS -> toolUpdateService.aptosNewVersion
            AptosTool.REVELA -> toolUpdateService.revelaNewVersion
            AptosTool.MOVEFMT -> toolUpdateService.movefmtNewVersion
        }
        if (newVersionAvailable == null) return null
        return EditorNotificationPanel().apply {
            text("New ${tool.id.capitalized()} is available: $newVersionAvailable")
            createActionLabel("Update ${tool.id.capitalized()}") {
                toolUpdateService.doToolUpdate(tool)
            }
            doNotShowAgainLabel(file)
        }
    }
}

class AptosUpdateAvailableNotification(project: Project): MvToolUpdateAvailableNotificationBase(project) {
    override val tool: AptosTool get() = AptosTool.APTOS
}
class RevelaUpdateAvailableNotification(project: Project): MvToolUpdateAvailableNotificationBase(project) {
    override val tool: AptosTool get() = AptosTool.REVELA
}
class MovefmtUpdateAvailableNotification(project: Project): MvToolUpdateAvailableNotificationBase(project) {
    override val tool: AptosTool get() = AptosTool.MOVEFMT
}
