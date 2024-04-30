package org.move.ide.notifications

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.settings.*
import org.move.lang.isMoveFile
import org.move.lang.isMoveTomlManifestFile
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.showSettingsDialog

class InvalidBlockchainCliConfiguration(project: Project): MvEditorNotificationProvider(project),
                                                           DumbAware {

    override val VirtualFile.disablingKey: String get() = NOTIFICATION_STATUS_KEY + path

    override fun createNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {
        if (isUnitTestMode) return null
        if (!(file.isMoveFile || file.isMoveTomlManifestFile)) return null
        @Suppress("UnstableApiUsage")
        if (!project.isTrusted()) return null
        if (isNotificationDisabled(file)) return null

        val blockchain = project.moveSettings.blockchain
        when (blockchain) {
            Blockchain.APTOS -> {
                if (project.aptosExecPath.isValidExecutable()) return null
            }
            Blockchain.SUI -> {
                if (project.suiExecPath.isValidExecutable()) return null
            }
        }

        return EditorNotificationPanel().apply {
            text = "$blockchain CLI path is not provided or invalid"
            createActionLabel("Configure") {
                project.showSettingsDialog<PerProjectMoveConfigurable>()
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
