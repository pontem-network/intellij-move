package org.move.openapiext

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.SystemInfo
import java.nio.file.Files
import java.nio.file.Path

const val PLUGIN_ID: String = "org.move.lang"

fun plugin(): IdeaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))!!

@Service(Service.Level.APP)
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

    fun getCurrentOS(): String {
        val openssl3 = service<OpenSSLInfoService>().openssl3
        return when {
            SystemInfo.isMac -> "MacOSX"
            SystemInfo.isWindows -> "Windows"
            else -> {
                if (openssl3) "Ubuntu-22.04" else "Ubuntu"
            }
        }
    }

    val bundledAptosCli: String?
        get() {
            val platform = getCurrentOS()
            val (os, binaryName) =
                when (platform) {
                    "Ubuntu-22.04" -> "ubuntu22" to "aptos"
                    "Ubuntu" -> "ubuntu" to "aptos"
                    "MacOSX" -> "macos" to "aptos"
                    "Windows" -> "windows" to "aptos.exe"
                    else -> error("unreachable")
                }
            val aptosCli = pluginDir().resolve("bin/$os/$binaryName").takeIf { Files.exists(it) } ?: return null
            return if (Files.isExecutable(aptosCli) || aptosCli.toFile().setExecutable(true)) {
                aptosCli.toString()
            } else {
                null
            }
        }
}
