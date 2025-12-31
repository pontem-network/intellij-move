package org.move.ide.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import org.move.cli.sdks.EndlessSdk
import org.move.cli.sdks.DownloadEndlessSdkDialog
import org.move.cli.sdks.DownloadEndlessSdkTask
import org.move.cli.sdks.sdksService

@Suppress("DialogTitleCapitalization")
class DownloadEndlessSDKAction: DumbAwareAction("Download pre-compiled binary from GitHub") {

    var onFinish: (EndlessSdk) -> Unit = { _ -> }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val sdkParametersDialog = DownloadEndlessSdkDialog(e.project)
        val isOk = sdkParametersDialog.showAndGet()
        if (isOk) {
            // download Endless SDK
            val sdkVersion = sdkParametersDialog.versionField.text

            // todo: show balloon error if no sdks dir set
            val sdksDir = sdksService().sdksDir ?: return

            val archive = EndlessSdk(sdksDir, sdkVersion)
            ProgressManager.getInstance()
                .run(DownloadEndlessSdkTask(archive, onFinish))
        }
    }

    companion object {
        fun create(onFinish: (EndlessSdk) -> Unit): DownloadEndlessSDKAction {
            val action = DownloadEndlessSDKAction()
            action.onFinish = onFinish
            return action
        }
    }
}
