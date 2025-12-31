package org.move.openapiext

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.system.CpuArch
import org.move.ide.notifications.logOrShowBalloon
import java.nio.file.Files
import java.nio.file.Path

const val PLUGIN_ID: String = "org.move.lang"

fun plugin(): IdeaPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))!!

enum class PlatformOS(val title: String, val shortTitle: String, val binaryName: String) {
    Windows("Windows", "windows", "endless.exe"),
    MacOS("MacOSX", "macos", "endless"),
    LinuxX86("Linux", "linux-x86", "endless"),
    LinuxArm64("Linux-Arm", "linux-arm64", "endless");
}

//val SUPPORTED_PLATFORMS: Set<PlatformOS> =
//    EnumSet.of(PlatformOS.Windows, PlatformOS.LinuxX86, PlatformOS.LinuxArm64)

object BundledEndlessManager {
    private fun pluginDir(): Path = plugin().pluginPath

    fun getCurrentOS(): PlatformOS {
        return when {
            SystemInfo.isMac -> PlatformOS.MacOS
            SystemInfo.isWindows -> PlatformOS.Windows
            CpuArch.CURRENT == CpuArch.ARM64 -> PlatformOS.LinuxArm64
            else -> PlatformOS.LinuxX86
        }
    }

    fun getBundledEndlessPath(): Path? {
        val platformOS = getCurrentOS()

        val os = platformOS.shortTitle
        val binaryName = platformOS.binaryName
        val bundledPath = pluginDir().resolve("bin/$os/$binaryName")
        if (!Files.exists(bundledPath)) {
            log.logOrShowBalloon(
                "Bundled Endless CLI Error: file `$bundledPath` does not exist"
            )
            return null
        }
        val isExecutable = Files.isExecutable(bundledPath)
        if (!isExecutable) {
            log.logOrShowBalloon(
                "Bundled Endless CLI Error: file `$bundledPath` is not an executable"
            )
            val set = bundledPath.toFile().setExecutable(true)
            if (!set) {
                log.logOrShowBalloon(
                    "Bundled Endless CLI Error: " +
                            "file `$bundledPath` cannot be made executable, not enough permissions"
                )
                return null
            }
        }
        return bundledPath
    }

    private val log: Logger = logger<BundledEndlessManager>()
}
