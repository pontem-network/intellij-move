package org.move.ide.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import org.move.cli.sdks.AptosSdk
import org.move.cli.sdks.AptosSdksSettingsService
import org.move.cli.sdks.DownloadAptosSdkDialog
import org.move.cli.sdks.DownloadAptosSdkTask

class DownloadAptosSDKAction: DumbAwareAction("Download from GitHub") {

    var onFinish: (AptosSdk) -> Unit = { _ -> }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val sdkParametersDialog = DownloadAptosSdkDialog(e.project)
        val isOk = sdkParametersDialog.showAndGet()
        if (isOk) {
            // download Aptos SDK
            val sdkVersion = sdkParametersDialog.versionField.text

            // todo: show balloon error if no sdks dir set
            val sdksDir = service<AptosSdksSettingsService>().sdksDir ?: return

            val archive = AptosSdk(sdksDir, sdkVersion)
            ProgressManager.getInstance()
                .run(DownloadAptosSdkTask(archive, onFinish))
        }
    }

    companion object {
        fun create(onFinish: (AptosSdk) -> Unit): DownloadAptosSDKAction {
            val action = DownloadAptosSDKAction()
            action.onFinish = onFinish
            return action
        }
    }
}
