package org.move.openapiext

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.SystemInfo
import org.move.ide.notifications.logOrShowBalloon
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import kotlin.LazyThreadSafetyMode.SYNCHRONIZED

const val PLUGIN_ID: String = "org.move.lang"

fun plugin(): IdeaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))!!

@Service(Service.Level.APP)
class OpenSSLInfoService {
    val isOpenSSL3: Lazy<Boolean> = lazy(SYNCHRONIZED) {
        if (SystemInfo.isWindows || SystemInfo.isMac) return@lazy false
        ApplicationManager.getApplication().executeOnPooledThread(Callable {
            val openSSLVersion = determineOpenSSLVersion()
            when {
                openSSLVersion.startsWith("OpenSSL 1") -> false
                else -> true
            }
        }).get()
    }

    private fun determineOpenSSLVersion(): String {
//        if (!isUnitTestMode) {
//            checkIsBackgroundThread()
//        }
        val commandLine = GeneralCommandLine("openssl", "version")
        return commandLine.execute()?.stdoutLines?.firstOrNull() ?: "OpenSSL 3.0.2"
    }
}

enum class PlatformOS(val title: String, val shortTitle: String, val binaryName: String) {
    Windows("Windows", "windows", "aptos.exe"),
    MacOS("MacOSX", "macos", "aptos"),
    Ubuntu("Ubuntu", "ubuntu", "aptos"),
    Ubuntu22("Ubuntu-22.04", "ubuntu22", "aptos");
}

val SUPPORTED_PLATFORMS: Set<PlatformOS> =
    EnumSet.of(PlatformOS.Windows, PlatformOS.Ubuntu, PlatformOS.Ubuntu22)

object BundledAptosManager {
    private fun pluginDir(): Path = plugin().pluginPath

    private val isOpenSSL3: Boolean get() = service<OpenSSLInfoService>().isOpenSSL3.value

    fun getCurrentOS(): PlatformOS {
        return when {
            SystemInfo.isMac -> PlatformOS.MacOS
            SystemInfo.isWindows -> PlatformOS.Windows
            else -> {
                if (isOpenSSL3) PlatformOS.Ubuntu22 else PlatformOS.Ubuntu
            }
        }
    }

    fun getBundledAptosPath(): Path? {
        val platformOS = getCurrentOS()

        val os = platformOS.shortTitle
        val binaryName = platformOS.binaryName
        val bundledPath = pluginDir().resolve("bin/$os/$binaryName")
        if (!Files.exists(bundledPath)) {
            log.logOrShowBalloon(
                "Bundled Aptos CLI Error: file `$bundledPath` does not exist"
            )
            return null
        }
        val isExecutable = Files.isExecutable(bundledPath)
        if (!isExecutable) {
            log.logOrShowBalloon(
                "Bundled Aptos CLI Error: file `$bundledPath` is not an executable"
            )
            val set = bundledPath.toFile().setExecutable(true)
            if (!set) {
                log.logOrShowBalloon(
                    "Bundled Aptos CLI Error: " +
                            "file `$bundledPath` cannot be made executable, not enough permissions"
                )
                return null
            }
        }
        return bundledPath
    }

    private val log: Logger = logger<BundledAptosManager>()
}
