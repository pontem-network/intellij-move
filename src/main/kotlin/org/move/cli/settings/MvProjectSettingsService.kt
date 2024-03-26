package org.move.cli.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.move.cli.settings.MvProjectSettingsService.MoveProjectSettings
import org.move.cli.settings.aptos.AptosExec
import org.move.stdext.exists
import org.move.stdext.isExecutableFile
import org.move.stdext.toPathOrNull
import java.nio.file.Path

enum class Blockchain {
    APTOS, SUI;

    override fun toString(): String = if (this == APTOS) "Aptos" else "Sui"
}

val Project.moveSettings: MvProjectSettingsService get() = service()

private const val SERVICE_NAME: String = "MoveProjectSettingsService_1"

@State(
    name = SERVICE_NAME,
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class MvProjectSettingsService(
    project: Project
):
    MvProjectSettingsServiceBase<MoveProjectSettings>(project, MoveProjectSettings()) {

    val blockchain: Blockchain get() = state.blockchain
    val aptosPath: String? get() = state.aptosPath
    val suiPath: String get() = state.suiPath

    val disableTelemetry: Boolean get() = state.disableTelemetry
    val foldSpecs: Boolean get() = state.foldSpecs
    val skipFetchLatestGitDeps: Boolean get() = state.skipFetchLatestGitDeps
    val dumpStateOnTestFailure: Boolean get() = state.dumpStateOnTestFailure

    // default values for settings
    class MoveProjectSettings(
        // null not Mac -> Bundled, null and Mac -> Local(""), not null -> Local(value)
        var blockchain: Blockchain = Blockchain.APTOS,
        var aptosPath: String? = if (AptosExec.isBundledSupportedForThePlatform()) null else "",
        var suiPath: String = "",
        var foldSpecs: Boolean = false,
        var disableTelemetry: Boolean = true,
        var debugMode: Boolean = false,
        var skipFetchLatestGitDeps: Boolean = false,
        var dumpStateOnTestFailure: Boolean = false,
    ): MvProjectSettingsBase<MoveProjectSettings>() {
        fun aptosExec(): AptosExec {
            val path = aptosPath
            return when (path) {
                null -> AptosExec.Bundled
                else -> AptosExec.LocalPath(path)
            }
        }

        override fun copy(): MoveProjectSettings {
            val state = MoveProjectSettings()
            state.copyFrom(this)
            return state
        }
    }

    override fun notifySettingsChanged(event: SettingsChangedEventBase<MoveProjectSettings>) {
        super.notifySettingsChanged(event)

        if (event.isChanged(MoveProjectSettings::foldSpecs)) {
            PsiManager.getInstance(project).dropPsiCaches()
        }
    }

    override fun createSettingsChangedEvent(
        oldEvent: MoveProjectSettings,
        newEvent: MoveProjectSettings
    ): SettingsChangedEvent = SettingsChangedEvent(oldEvent, newEvent)

    class SettingsChangedEvent(
        oldState: MoveProjectSettings,
        newState: MoveProjectSettings
    ): SettingsChangedEventBase<MoveProjectSettings>(oldState, newState)
}

val Project.aptosExec: AptosExec get() = this.moveSettings.state.aptosExec()

val Project.aptosPath: Path? get() = this.aptosExec.toPathOrNull()

val Project.suiPath: Path? get() = this.moveSettings.suiPath.toPathOrNull()

fun Path?.isValidExecutable(): Boolean {
    return this != null
            && this.toString().isNotBlank()
            && this.exists()
            && this.isExecutableFile()
}

val Project.isDebugModeEnabled: Boolean get() = this.moveSettings.state.debugMode

fun <T> Project.debugErrorOrFallback(message: String, fallback: T): T {
    if (this.isDebugModeEnabled) {
        error(message)
    }
    return fallback
}

fun <T> Project.debugErrorOrFallback(message: String, cause: Throwable?, fallback: () -> T): T {
    if (this.isDebugModeEnabled) {
        throw IllegalStateException(message, cause)
    }
    return fallback()
}
