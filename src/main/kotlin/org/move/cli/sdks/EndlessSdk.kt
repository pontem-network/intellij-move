package org.move.cli.sdks

import com.intellij.openapi.util.SystemInfo
import org.move.openapiext.BundledEndlessManager
import java.io.File

data class EndlessSdk(val sdksDir: String, val version: String) {
    val githubArchiveUrl: String
        get() {
            return "https://github.com/endless-labs/endless-release/releases/download" +
                    "/v$version/$githubArchiveFileName"
        }

    val githubArchiveFileName: String
        get() {
            return "endless-$version-${BundledEndlessManager.getCurrentOS().title}.zip"
        }

    val targetFile: File
        get() = File(sdksDir, if (SystemInfo.isWindows) "endless-$version.exe" else "endless-$version")
}
