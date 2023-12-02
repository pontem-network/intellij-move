package org.move.cli.settings

import com.intellij.openapi.util.SystemInfo
import org.move.cli.runConfigurations.aptos.AptosCliExecutor
import org.move.openapiext.PluginPathManager
import org.move.stdext.toPathOrNull

sealed class AptosExec {
    abstract val execPath: String

    object Bundled: AptosExec() {
        override val execPath: String
            get() = PluginPathManager.bundledAptosCli ?: ""
    }

    data class LocalPath(override val execPath: String): AptosExec()

    fun pathOrNull() = this.execPath.toPathOrNull()

    fun toExecutor(): AptosCliExecutor? =
        execPath.toPathOrNull()?.let { AptosCliExecutor(it) }

    fun pathToSettingsFormat(): String? =
        when (this) {
            is LocalPath -> this.execPath
            is Bundled -> null
        }

    companion object {
        fun fromSettingsFormat(aptosPath: String?): AptosExec =
            when (aptosPath) {
                null -> Bundled
                else -> LocalPath(aptosPath)
            }

        fun isBundledSupported(): Boolean = !SystemInfo.isMac
    }
}
