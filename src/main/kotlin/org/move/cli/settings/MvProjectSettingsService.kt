package org.move.cli.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import org.move.bytecode.createDisposableOnFileChange
import org.move.cli.runConfigurations.aptos.Aptos
import org.move.cli.settings.MvProjectSettingsService.MoveProjectSettings
import org.move.cli.settings.aptos.AptosExecType
import org.move.stdext.exists
import org.move.stdext.isExecutableFile
import java.nio.file.Path

private const val SERVICE_NAME: String = "MoveProjectSettingsService_1"

@State(
    name = SERVICE_NAME,
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class MvProjectSettingsService(
    project: Project
):
    MvProjectSettingsServiceBase<MoveProjectSettings>(project, MoveProjectSettings()) {

    val aptosExecType: AptosExecType get() = state.aptosExecType
    val localAptosPath: String? get() = state.localAptosPath

    val fetchAptosDeps: Boolean get() = state.fetchAptosDeps

    val disableTelemetry: Boolean get() = state.disableTelemetry
    val skipFetchLatestGitDeps: Boolean get() = state.skipFetchLatestGitDeps
    val dumpStateOnTestFailure: Boolean get() = state.dumpStateOnTestFailure

    val enableResourceAccessControl: Boolean get() = state.enableResourceAccessControl
    val addCompilerV2CLIFlags: Boolean get() = state.addCompilerV2CLIFlags

    // default values for settings
    class MoveProjectSettings: MvProjectSettingsBase<MoveProjectSettings>() {
        @AffectsMoveProjectsMetadata
        var aptosExecType: AptosExecType by enum(defaultAptosExecType)

        @AffectsMoveProjectsMetadata
        var localAptosPath: String? by string()

        @AffectsParseTree
        var enableResourceAccessControl: Boolean by property(false)

        @AffectsMoveProjectsMetadata
        var fetchAptosDeps: Boolean by property(false)

        var disableTelemetry: Boolean by property(true)

        // change to true here to not annoy the users with constant updates
        var skipFetchLatestGitDeps: Boolean by property(true)
        var dumpStateOnTestFailure: Boolean by property(false)

        var addCompilerV2CLIFlags: Boolean by property(false)

        override fun copy(): MoveProjectSettings {
            val state = MoveProjectSettings()
            state.copyFrom(this)
            return state
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
                if (AptosExecType.isPreCompiledSupportedForThePlatform) AptosExecType.BUNDLED else AptosExecType.LOCAL
    }
}

val Project.moveSettings: MvProjectSettingsService get() = service()

fun Project.getAptosCli(parentDisposable: Disposable? = null): Aptos? {
    val aptosExecPath =
        AptosExecType.aptosExecPath(
            this.moveSettings.aptosExecType,
            this.moveSettings.localAptosPath
        )
    val aptos = aptosExecPath?.let { Aptos(it, parentDisposable) }
    return aptos
}

fun Project.getAptosCliDisposedOnFileChange(file: VirtualFile): Aptos? {
    val anyChangeDisposable = this.createDisposableOnFileChange(file)
    return this.getAptosCli(anyChangeDisposable)
}

val Project.aptosExecPath: Path? get() = this.getAptosCli()?.cliLocation

fun Path?.isValidExecutable(): Boolean {
    return this != null
            && this.toString().isNotBlank()
            && this.exists()
            && this.isExecutableFile()
}

fun isDebugModeEnabled(): Boolean = Registry.`is`("org.move.debug.enabled")

fun <T> debugErrorOrFallback(message: String, fallback: T): T {
    if (isDebugModeEnabled()) {
        error(message)
    }
    return fallback
}

fun <T> debugErrorOrFallback(message: String, cause: Throwable?, fallback: () -> T): T {
    if (isDebugModeEnabled()) {
        throw IllegalStateException(message, cause)
    }
    return fallback()
}
