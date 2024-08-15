package org.move.cli.sdks

import com.intellij.openapi.util.SystemInfo
import org.move.openapiext.BundledAptosManager
import java.io.File

data class AptosSdk(val sdksDir: String, val version: String) {
    val githubArchiveUrl: String
        get() {
            return "https://github.com/aptos-labs/aptos-core/releases/download" +
                    "/aptos-cli-v$version/$githubArchiveFileName"
        }

    val githubArchiveFileName: String
        get() {
            return "aptos-cli-$version-${BundledAptosManager.getCurrentOS().title}-x86_64.zip"
        }

    val targetFile: File
        get() = File(sdksDir, if (SystemInfo.isWindows) "aptos-$version.exe" else "aptos-$version")
}
