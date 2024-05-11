package org.move.cli.sdks

import com.intellij.openapi.components.*
import com.intellij.util.SystemProperties
import org.move.cli.sdks.AptosSdksSettingsService.AptosSdksSettings
import java.nio.file.Paths

private const val SERVICE_NAME: String = "AptosSdksSettingsService"

@State(
    name = SERVICE_NAME,
    storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)],
)
@Service(Service.Level.APP)
class AptosSdksSettingsService: SimplePersistentStateComponent<AptosSdksSettings>(AptosSdksSettings()) {

    val sdksDir: String? get() = this.state.sdksDir

    class AptosSdksSettings: BaseState() {
        // null is empty string
        var sdksDir: String? by string(defaultValue = DEFAULT_SDKS_DIR)

        fun copy(): AptosSdksSettings {
            val state = AptosSdksSettings()
            state.copyFrom(this)
            return state
        }
    }

    companion object {
        private val DEFAULT_SDKS_DIR =
            Paths.get(SystemProperties.getUserHome(), "aptos-clis").toAbsolutePath().toString()
    }
}