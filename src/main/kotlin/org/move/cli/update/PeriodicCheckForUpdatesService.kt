package org.move.cli.update

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
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
import org.move.cli.settings.getAptosCli
import org.move.cli.update.PeriodicCheckForUpdatesService.LastTimeChecked
import org.move.ide.notifications.showBalloon
import org.move.ide.notifications.showDebugBalloon
import org.move.ide.notifications.updateAllNotifications
import org.move.stdext.capitalized
import org.move.stdext.now
import org.move.stdext.unwrapOrElse
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@State(
    name = "org.move.PeriodicCheckForUpdatesService2",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
@Service(Service.Level.PROJECT)
class PeriodicCheckForUpdatesService(
    private val project: Project,
    cs: CoroutineScope
):
    SimplePersistentStateComponent<LastTimeChecked>(LastTimeChecked()),
    Disposable.Default {

    val aptosNewVersion: String? get() = state.aptosNewVersion
    val revelaNewVersion: String? get() = state.revelaNewVersion
    val movefmtNewVersion: String? get() = state.movefmtNewVersion

    class LastTimeChecked: BaseState() {
        var lastChecked: Long by property(0.toLong())
        var aptosNewVersion: String? by string()
        var revelaNewVersion: String? by string()
        var movefmtNewVersion: String? by string()
    }

    init {
        cs.launch {
            delay(INITIAL_POLLING_DELAY)
            while (true) {
                val now = now()
                if (now - state.lastChecked.seconds > TIME_BETWEEN_UPDATE_CHECKS) {
                    checkForToolUpdate(AptosTool.APTOS)
                    checkForToolUpdate(AptosTool.REVELA)
                    checkForToolUpdate(AptosTool.MOVEFMT)

                    state.lastChecked = now.inWholeSeconds
                    updateAllNotifications(project)
                }
                delay(TIME_BETWEEN_POLLING_FOR_UPDATE_CHECK)
            }
        }
    }

    fun checkForToolUpdate(tool: AptosTool): UpdateCheckResult? {
        val aptos = project.getAptosCli(parentDisposable = this) ?: return null // aptos not configured
        val checkResult =
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
        val newVersion = when (checkResult) {
            is UpdateCheckResult.UpdateIsAvailable -> checkResult.version
            else -> null
        }
        setToolVersion(tool, newVersion)
        project.showDebugBalloon(
            "tool update: ${tool.id}",
            "$checkResult\n$state",
            NotificationType.INFORMATION
        )
        return checkResult
    }

    fun doToolUpdate(tool: AptosTool) {
        val cancelDisposable = Disposer.newDisposable()
        val aptos = project.getAptosCli(cancelDisposable) ?: return

        val updateService = this
        // blocks
        object: Task.Backgroundable(project, "Updating ${tool.id.capitalized()} to the latest version", true) {
            override fun onCancel() {
                Disposer.dispose(cancelDisposable)
            }

            override fun run(indicator: ProgressIndicator) {
                val indicatorPrefix = "${tool.id.capitalized()} update:"
                indicator.text = "$indicatorPrefix: fetching latest version..."
                val aptosOutput = aptos.doToolUpdate(
                    tool,
                    processListener = object: ProcessListener {
                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            indicator.checkCanceled()
                            val line = event.text
                            when {
                                line.contains("Downloading") ->
                                    indicator.text = "$indicatorPrefix: ${line.lowercase()}"
                                line.contains("Extracting") ->
                                    indicator.text = "$indicatorPrefix: ${line.lowercase()}"
                            }
                        }
                    })
                    .unwrapOrElse {
                        project.showBalloon("aptos update", it.message ?: "", NotificationType.ERROR)
                        return
                    }

                indicator.checkCanceled()

                when (val exitStatus = aptosOutput.exitStatus) {
                    is AptosExitStatus.Result -> {
                        project.showBalloon(
                            "aptos update success",
                            exitStatus.message,
                            NotificationType.INFORMATION
                        )
                        updateService.setToolVersion(tool, null)
                        updateAllNotifications(project)
                    }
                    is AptosExitStatus.Error ->
                        project.showBalloon("aptos update failure", exitStatus.message, NotificationType.ERROR)
                    is AptosExitStatus.Malformed ->
                        project.showBalloon(
                            "aptos update malformed",
                            exitStatus.message,
                            NotificationType.ERROR
                        )
                }
            }
        }.queue()
    }

    fun setToolVersion(tool: AptosTool, version: String?) {
        when (tool) {
            AptosTool.APTOS -> state.aptosNewVersion = version
            AptosTool.REVELA -> state.revelaNewVersion = version
            AptosTool.MOVEFMT -> state.movefmtNewVersion = version
        }
    }

    companion object {
        private val INITIAL_POLLING_DELAY = 5.seconds

        // how often to ask the service whether it's time to run the update check
        private val TIME_BETWEEN_POLLING_FOR_UPDATE_CHECK = 10.seconds

        // time between update checks for tools
        private val TIME_BETWEEN_UPDATE_CHECKS = 1.hours

        val isEnabled get() = AdvancedSettings.getBoolean(PERIODIC_UPDATE_CHECK_SETTING_KEY)

        private const val PERIODIC_UPDATE_CHECK_SETTING_KEY: String = "org.move.aptos.update.check"
    }
}

val Project.toolUpdateService: PeriodicCheckForUpdatesService get() = service()

enum class AptosTool(val id: String) {
    APTOS("aptos"), REVELA("revela"), MOVEFMT("movefmt");
}

sealed class UpdateCheckResult {
    data class UpToDate(val version: String): UpdateCheckResult()
    data class UpdateIsAvailable(val version: String): UpdateCheckResult()
    data class MalformedResult(val resultText: String): UpdateCheckResult()
    data class UpdateError(val errorText: String): UpdateCheckResult()
}