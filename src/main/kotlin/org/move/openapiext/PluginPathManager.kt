package org.move.openapiext

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.*
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.SystemInfo
import org.move.openapiext.common.isUnitTestMode
import java.nio.file.Files
import java.nio.file.Path

const val PLUGIN_ID: String = "org.move.lang"

fun plugin(): IdeaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))!!

class OpenSSLInfoService {
    var openssl3: Boolean = true

    init {
        if (!SystemInfo.isWindows) {
            val fut = ApplicationManager.getApplication().executeOnPooledThread {
                val openSSLVersion = determineOpenSSLVersion()
                when {
                    openSSLVersion.startsWith("OpenSSL 1") -> openssl3 = false
                    else -> openssl3 = true
                }
            }
            // blocks
            fut.get()
        }
    }

    private fun determineOpenSSLVersion(): String {
//        if (!isUnitTestMode) {
//            checkIsBackgroundThread()
//        }
        return GeneralCommandLine("openssl", "version").execute()?.stdoutLines?.firstOrNull()
            ?: "OpenSSL 3.0.2"
    }
}

object PluginPathManager {
    private fun pluginDir(): Path = plugin().pluginPath

    val bundledAptosCli: String?
        get() {
            val openssl3 = service<OpenSSLInfoService>().openssl3
            val (os, binaryName) = when {
                SystemInfo.isMac -> "macos" to "aptos"
                SystemInfo.isWindows -> "windows" to "aptos.exe"
                else -> {
                    val platform = if (openssl3) "ubuntu22" else "ubuntu"
                    platform to "aptos"
                }
            }
            val aptosCli = pluginDir().resolve("bin/$os/$binaryName").takeIf { Files.exists(it) } ?: return null
            return if (Files.isExecutable(aptosCli) || aptosCli.toFile().setExecutable(true)) {
                aptosCli.toString()
            } else {
                null
            }
        }
}
