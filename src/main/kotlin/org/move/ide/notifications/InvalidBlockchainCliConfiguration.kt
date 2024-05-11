package org.move.ide.notifications

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.settings.*
import org.move.cli.settings.Blockchain.SUI
import org.move.cli.settings.Blockchain.APTOS
import org.move.cli.settings.aptos.AptosExecType.LOCAL
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
            APTOS -> {
                if (project.aptosExecPath.isValidExecutable()) return null
            }
            SUI -> {
                if (project.suiExecPath.isValidExecutable()) return null
            }
        }

        val blockchainCliFromPATH = Blockchain.blockchainCliFromPATH(blockchain.cliName())?.toString()
        return EditorNotificationPanel().apply {
            text = "$blockchain CLI path is not provided or invalid"
            if (blockchainCliFromPATH != null) {
                createActionLabel("Set to \"$blockchainCliFromPATH\"") {
                    project.moveSettings.modify {
                        when (blockchain) {
                            APTOS -> {
                                it.aptosExecType = LOCAL
                                it.localAptosPath = blockchainCliFromPATH
                            }
                            SUI -> {
                                it.localSuiPath = blockchainCliFromPATH
                            }
                        }
                    }
                }
            }
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
