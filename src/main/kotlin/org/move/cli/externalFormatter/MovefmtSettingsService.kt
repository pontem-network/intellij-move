package org.move.cli.externalFormatter

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.components.Service.Level.PROJECT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.util.text.nullize
import org.move.cli.externalFormatter.MoveFmtSettingsService.MoveFmtSettings
import org.move.cli.settings.MvProjectSettingsServiceBase
import org.move.cli.settings.isValidExecutable
import org.move.cli.tools.Movefmt

private const val SERVICE_NAME: String = "org.move.MoveFmtSettingsService"

@State(
    name = SERVICE_NAME,
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
@Service(PROJECT)
class MoveFmtSettingsService(
    project: Project,
): MvProjectSettingsServiceBase<MoveFmtSettings>(project, MoveFmtSettings()) {

    val useMovefmt: Boolean get() = state.useMovefmt
//    val runMovefmtOnSave: Boolean get() = state.runMovefmtOnSave

    val movefmtPath: String? get() = state.movefmtPath.nullize()
    val additionalArguments: String get() = state.additionalArguments
    val envs: Map<String, String> get() = state.envs

    class MoveFmtSettings: MvProjectSettingsBase<MoveFmtSettings>() {
        var useMovefmt by property(false)
//        var runMovefmtOnSave by property(false)

        var movefmtPath by property("") { it.isEmpty() }
        var additionalArguments by property("") { it.isEmpty() }
        var envs by map<String, String>()

        override fun copy(): MoveFmtSettings {
            val state = MoveFmtSettings()
            state.copyFrom(this)
            return state
        }
    }

    override fun createSettingsChangedEvent(
        oldEvent: MoveFmtSettings,
        newEvent: MoveFmtSettings
    ): SettingsChangedEvent = SettingsChangedEvent(oldEvent, newEvent)

    class SettingsChangedEvent(
        oldState: MoveFmtSettings,
        newState: MoveFmtSettings
    ): SettingsChangedEventBase<MoveFmtSettings>(oldState, newState)
}

val Project.movefmtSettings: MoveFmtSettingsService get() = service<MoveFmtSettingsService>()

fun Project.getMovefmt(disposable: Disposable): Movefmt? {
    val settings = this.movefmtSettings
    val movefmtPath = settings.movefmtPath?.toNioPathOrNull()?.takeIf { it.isValidExecutable() } ?: return null
    return Movefmt(movefmtPath, disposable)
}

