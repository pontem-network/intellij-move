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
import org.move.cli.runConfigurations.endless.Endless
import org.move.cli.settings.MvProjectSettingsService.MoveProjectSettings
import org.move.cli.settings.endless.EndlessExecType
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

    val endlessPath: String? get() = state.endlessPath

//    val fetchEndlessDeps: Boolean get() = state.fetchEndlessDeps

    val disableTelemetry: Boolean get() = state.disableTelemetry
    val testsExtraArgs: List<String>
        get() {
            return state.extraTestArgs.orEmpty().split(" ")
        }

    val enableMove2: Boolean get() = state.enableMove2
    val disabledMove2: Boolean get() = !state.enableMove2

    val enableReceiverStyleFunctions: Boolean get() = enableMove2
    val enableIndexExpr: Boolean get() = enableMove2
    val enablePublicPackage: Boolean get() = enableMove2

    // default values for settings
    class MoveProjectSettings: MvProjectSettingsBase<MoveProjectSettings>() {
        @AffectsMoveProjectsMetadata
        var endlessPath: String? by string()

        @AffectsMoveProjectsMetadata
        var fetchEndlessDeps: Boolean by property(false)

        var disableTelemetry: Boolean by property(true)

        var extraTestArgs: String? by string("--dev --skip-fetch-latest-git-deps")

        @AffectsHighlighting
        var enableMove2: Boolean by property(true)

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
}

val Project.moveSettings: MvProjectSettingsService get() = service()

val Project.endlessCliPath: Path?
    get() {
        val settings = this.moveSettings
        return EndlessExecType.endlessCliPath(settings.endlessPath)
    }

fun Project.getEndlessCli(
    parentDisposable: Disposable? = null
): Endless? =
    this.endlessCliPath?.let { Endless(it, parentDisposable) }

val Project.isEndlessConfigured: Boolean get() = this.getEndlessCli() != null

fun Project.getEndlessCliDisposedOnFileChange(file: VirtualFile): Endless? {
    val anyChangeDisposable = this.createDisposableOnFileChange(file)
    return this.getEndlessCli(anyChangeDisposable)
}

fun Path?.isValidExecutable(): Boolean {
    return this != null
            && this.toString().isNotBlank()
            && this.exists()
            && this.isExecutableFile()
}

fun isDebugModeEnabled(): Boolean = Registry.`is`("org.move.debug.enabled")
fun isTypeUnknownAsError(): Boolean = Registry.`is`("org.move.types.highlight.unknown.as.error")

fun debugError(message: String) {
    if (isDebugModeEnabled()) {
        error(message)
    }
}

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
