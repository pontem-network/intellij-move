package org.move.cli.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.move.cli.settings.MvProjectSettingsService.MoveProjectSettings
import org.move.cli.settings.aptos.AptosExecType
import org.move.stdext.exists
import org.move.stdext.isExecutableFile
import org.move.stdext.toPathOrNull
import org.move.utils.EnvUtils
import java.nio.file.Path

enum class Blockchain {
    APTOS, SUI;

    override fun toString(): String = if (this == APTOS) "Aptos" else "Sui"

    companion object {
        fun aptosFromPATH(): String? {
            // TODO: run --version and check whether it's a real Aptos CLI executable
            return EnvUtils.findInPATH("aptos")?.toAbsolutePath()?.toString()
        }

        fun suiFromPATH(): String? {
            // TODO: same as in Aptos
            return EnvUtils.findInPATH("sui")?.toAbsolutePath()?.toString()
        }
    }
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

    val aptosExecType: AptosExecType get() = state.aptosExecType
    val localAptosPath: String? get() = state.localAptosPath
    val localSuiPath: String? get() = state.localSuiPath

    val disableTelemetry: Boolean get() = state.disableTelemetry
    val foldSpecs: Boolean get() = state.foldSpecs
    val skipFetchLatestGitDeps: Boolean get() = state.skipFetchLatestGitDeps
    val dumpStateOnTestFailure: Boolean get() = state.dumpStateOnTestFailure

    val aptosExecPath: String get() = AptosExecType.aptosPath(aptosExecType, localAptosPath)

    // default values for settings
    class MoveProjectSettings: MvProjectSettingsBase<MoveProjectSettings>() {
        var blockchain: Blockchain by enum(Blockchain.APTOS)

        var aptosExecType: AptosExecType by enum(defaultAptosExecType)

        var localAptosPath: String? by string()
        var localSuiPath: String? by string()

        var foldSpecs: Boolean by property(false)
        var disableTelemetry: Boolean by property(true)
        var debugMode: Boolean by property(false)
        var skipFetchLatestGitDeps: Boolean by property(false)
        var dumpStateOnTestFailure: Boolean by property(false)

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

    companion object {
        private val defaultAptosExecType
            get() =
                if (AptosExecType.isBundledSupportedForThePlatform) AptosExecType.BUNDLED else AptosExecType.LOCAL;
    }
}

val Project.aptosExecPath: Path? get() = this.moveSettings.aptosExecPath.toPathOrNull()

val Project.suiExecPath: Path? get() = this.moveSettings.localSuiPath?.toPathOrNull()

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
