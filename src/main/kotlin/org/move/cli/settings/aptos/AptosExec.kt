package org.move.cli.settings.aptos

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.SystemInfo
import org.move.cli.settings.MoveProjectSettingsService
import org.move.cli.settings.isValidExecutable
import org.move.openapiext.PluginPathManager
import org.move.stdext.toPathOrNull

sealed class AptosExec {
    abstract val execPath: String

    object Bundled: AptosExec() {
        override val execPath: String
            get() = PluginPathManager.bundledAptosCli ?: ""
    }

    data class LocalPath(override val execPath: String): AptosExec()

    fun isValid(): Boolean {
        if (this is Bundled && !isBundledSupportedForThePlatform()) return false
        return this.toPathOrNull().isValidExecutable()
    }

    fun toPathOrNull() = this.execPath.toPathOrNull()

    fun pathToSettingsFormat(): String? =
        when (this) {
            is LocalPath -> this.execPath
            is Bundled -> null
        }

    companion object {
        fun default(): AptosExec {
            // Don't use `Project.moveSettings` here because `getService` can return `null`
            // for default project after dynamic plugin loading. As a result, you can get
            // `java.lang.IllegalStateException`. So let's handle it manually:
            val defaultProjectSettings =
                ProjectManager.getInstance().defaultProject.getService(MoveProjectSettingsService::class.java)

            val defaultProjectAptosExec = defaultProjectSettings?.state?.aptosExec()
            return defaultProjectAptosExec
                ?: if (isBundledSupportedForThePlatform()) Bundled else LocalPath("")
        }

        fun isBundledSupportedForThePlatform(): Boolean = !SystemInfo.isMac
    }
}
