package org.move.cli.update

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.move.cli.runConfigurations.aptos.AptosExitStatus
import org.move.cli.runConfigurations.aptos.AptosTool
import org.move.cli.runConfigurations.aptos.UpdateCheckResult
import org.move.cli.settings.getAptosCli
import org.move.cli.update.PeriodicCheckForUpdatesService.LastTimeChecked
import org.move.ide.notifications.showBalloon
import org.move.ide.notifications.showDebugBalloon
import org.move.stdext.now
import org.move.stdext.unwrapOrElse
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

//@State(
//    name = "org.move.PeriodicCheckForUpdatesService",
//    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
//)
//@Service(Service.Level.PROJECT)
class PeriodicCheckForUpdatesService(private val project: Project, cs: CoroutineScope):
    SimplePersistentStateComponent<LastTimeChecked>(LastTimeChecked()),
    Disposable.Default {

    class LastTimeChecked: BaseState() {
        var lastChecked: Long by property(0.toLong())

        var aptosLastResponseText: String? by string()
        var revelaLastResponseText: String? by string()
        var moveFmtLastResponseText: String? by string()
    }

    init {
        cs.launch {
            delay(INITIAL_POLLING_DELAY)
            while (true) {
                val now = now()
                if (now - state.lastChecked.seconds > TIME_BETWEEN_UPDATE_CHECKS) {
                    checkForToolUpdate(AptosTool.APTOS)
                    state.lastChecked = now.inWholeSeconds
                }
                delay(TIME_BETWEEN_POLLING_FOR_UPDATE_CHECK)
            }
        }
    }

    fun checkForToolUpdate(tool: AptosTool): UpdateCheckResult? {
        val aptos = project.getAptosCli(parentDisposable = this) ?: return null // aptos not configured
        val updateResult =
            aptos.checkForToolUpdate(tool).unwrapOrElse {
                // command wasn't started or return cannot be deserialized
                it.printStackTrace()
                project.showDebugBalloon(
                    "Error",
                    "error in checkForToolUpdate(), see stacktrace",
                    NotificationType.INFORMATION
                )
                return null
            }
//        val responseText = when (updateResult) {
//            is UpdateCheckResult.UpToDate -> "up-to-date"
//            is UpdateCheckResult.UpdateIsAvailable -> "update is available: ${updateResult.version}"
//            else -> "error"
//        }
//        state.moveFmtLastResponseText = responseText
//        project.showDebugBalloon(responseText, NotificationType.INFORMATION)
        return updateResult
    }

    fun doToolUpdate(tool: AptosTool) {
        val cancelDisposable = Disposer.newDisposable()
        val aptos = project.getAptosCli(cancelDisposable) ?: return

        // blocks
        object: Task.Modal(project, "Updating ${tool.id} to the latest version", true) {

            override fun onCancel() {
                Disposer.dispose(cancelDisposable)
            }

            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Fetching latest version..."
                val aptosOutput = aptos.doToolUpdate(
                    tool,
                    processListener = object: ProcessListener {
                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            indicator.checkCanceled()
                            val line = event.text
                            when {
                                line.contains("Downloading") -> indicator.text = line
                                line.contains("Extracting") -> indicator.text = line
                            }
                        }
                    })
                    .unwrapOrElse {
                        project.showBalloon("aptos update", it.message ?: "", NotificationType.ERROR)
                        return
                    }

                when (val exitStatus = aptosOutput.exitStatus) {
                    is AptosExitStatus.Result ->
                        project.showBalloon("aptos update success", exitStatus.message, NotificationType.INFORMATION)
                    is AptosExitStatus.Error ->
                        project.showBalloon("aptos update failure", exitStatus.message, NotificationType.ERROR)
                    is AptosExitStatus.Malformed ->
                        project.showBalloon("aptos update malformed", exitStatus.message, NotificationType.ERROR)
                }
            }
        }.queue()
    }

    companion object {
        private val INITIAL_POLLING_DELAY = 5.seconds
        // how often to ask the service whether it's time to run the update check
        private val TIME_BETWEEN_POLLING_FOR_UPDATE_CHECK = 10.seconds
        // time between aptos update checks for tools
        private val TIME_BETWEEN_UPDATE_CHECKS = 12.hours

        val isEnabled get() = AdvancedSettings.getBoolean(PERIODIC_UPDATE_CHECK_SETTING_KEY)

        private const val PERIODIC_UPDATE_CHECK_SETTING_KEY: String = "org.move.aptos.update"
    }
}

enum class AptosTool(val id: String) {
    APTOS("aptos"), REVELA("revela"), MOVEFMT("movefmt");
}

sealed class UpdateCheckResult {
    data class UpToDate(val version: String): UpdateCheckResult()
    data class UpdateIsAvailable(val version: String): UpdateCheckResult()
    data class MalformedResult(val resultText: String): UpdateCheckResult()
    data class UpdateError(val errorText: String): UpdateCheckResult()
}