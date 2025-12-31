package org.move.cli.sdks

import com.intellij.openapi.components.*
import com.intellij.util.SystemProperties
import org.move.cli.sdks.EndlessSdksSettingsService.EndlessSdksSettings
import java.nio.file.Paths

private const val SERVICE_NAME: String = "EndlessSdksSettingsService"

@State(
    name = SERVICE_NAME,
    storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)],
)
@Service(Service.Level.APP)
class EndlessSdksSettingsService: SimplePersistentStateComponent<EndlessSdksSettings>(EndlessSdksSettings()) {

    val sdksDir: String? get() = this.state.sdksDir
    val endlessSdkPaths: List<String> get() = this.state.endlessSdkPaths

    class EndlessSdksSettings: BaseState() {
        // null is empty string
        var sdksDir: String? by string(defaultValue = DEFAULT_SDKS_DIR)

        var endlessSdkPaths: MutableList<String> by list()

        fun copy(): EndlessSdksSettings {
            val state = EndlessSdksSettings()
            state.copyFrom(this)
            return state
        }
    }

    companion object {
        private val DEFAULT_SDKS_DIR =
            Paths.get(SystemProperties.getUserHome(), "endless-clis").toAbsolutePath().toString()
    }
}

fun sdksService(): EndlessSdksSettingsService = service()