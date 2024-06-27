package org.move.ide.notifications

import com.intellij.ide.impl.isTrusted
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import org.move.cli.settings.PerProjectAptosConfigurable
import org.move.cli.settings.aptos.AptosExecType.LOCAL
import org.move.cli.settings.aptosExecPath
import org.move.cli.settings.isValidExecutable
import org.move.cli.settings.moveSettings
import org.move.lang.isMoveFile
import org.move.lang.isMoveTomlManifestFile
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.showSettingsDialog
import org.move.stdext.getCliFromPATH

class InvalidBlockchainCliConfiguration(project: Project): MvEditorNotificationProvider(project),
                                                           DumbAware {

    override val VirtualFile.disablingKey: String get() = NOTIFICATION_STATUS_KEY + path

    override fun createNotificationPanel(file: VirtualFile, project: Project): EditorNotificationPanel? {
        if (isUnitTestMode) return null
        if (!(file.isMoveFile || file.isMoveTomlManifestFile)) return null
        @Suppress("UnstableApiUsage")
        if (!project.isTrusted()) return null
        if (isNotificationDisabled(file)) return null

        if (project.aptosExecPath.isValidExecutable()) return null

        val aptosCliFromPATH = getCliFromPATH("aptos")?.toString()
        return EditorNotificationPanel().apply {
            text = "Aptos CLI path is not provided or invalid"
            if (aptosCliFromPATH != null) {
                createActionLabel("Set to \"$aptosCliFromPATH\"") {
                    project.moveSettings.modify {
                        it.aptosExecType = LOCAL
                        it.localAptosPath = aptosCliFromPATH
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

    companion object {
        private const val NOTIFICATION_STATUS_KEY = "org.move.hideMoveNotifications"
    }
}
