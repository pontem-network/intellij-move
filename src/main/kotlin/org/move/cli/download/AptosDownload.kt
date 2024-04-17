package org.move.cli.download

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkDownloaderLogger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.Urls
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile

data class AptosItem(val version: String) {
    val url
        get() =
            "https://github.com/aptos-labs/aptos-core/releases/download/aptos-cli-v$version/aptos-cli-$version-$currentPlatform-x86_64.zip"
    val archiveVersionInfo get() = "$version-$currentPlatform-x86_64.zip"

    val executableName get() = "aptos-cli-$version"
    val archiveNameNoExtension get() = "aptos-cli-$version-$currentPlatform"

    val path: Path get() = AptosInstaller.installDir.resolve(executableName)

    override fun toString(): String = executableName

    companion object {
        private val currentPlatform: String
            get() {
                return "Ubuntu-22.04"
            }
    }
}

@Service
class AptosInstaller {
    fun installAptosCli(aptosItem: AptosItem, indicator: ProgressIndicator, onFinish: (AptosItem) -> Unit = { _ -> }) {
        indicator.text = "Installing ${aptosItem.archiveNameNoExtension}..."

        val url = Urls.parse(aptosItem.url, false) ?: error("Cannot parse download URL: ${aptosItem.url}")
        if (!url.scheme.equals("https", ignoreCase = true)) {
            error("URL must use https:// protocol, but was: $url")
        }

        indicator.text2 = "Downloading"
        val tempDownloadFile =
            Paths.get(
                PathManager.getTempPath(),
                FileUtil.sanitizeFileName("aptos-cli-${System.nanoTime()}-${aptosItem.archiveVersionInfo}")
            )
        val tempExtractionDir = tempDownloadFile.parent.resolve(aptosItem.archiveNameNoExtension)
        val targetFilePath = aptosItem.path
        try {
            try {
                HttpRequests.request(aptosItem.url)
                    .productNameAsUserAgent()
                    .saveToFile(tempDownloadFile.toFile(), indicator)

                if (!tempDownloadFile.isRegularFile()) {
                    throw RuntimeException("Downloaded file does not exist: $tempDownloadFile")
                }
            } catch (t: Throwable) {
                if (t is ControlFlowException) throw t
                throw RuntimeException(
                    "Failed to download ${aptosItem.archiveNameNoExtension} from $url. ${t.message}",
                    t
                )
            }

//            val sizeDiff = runCatching { Files.size(downloadFile) - item.archiveSize }.getOrNull()
//            if (sizeDiff != 0L) {
//                throw RuntimeException("The downloaded ${item.fullPresentationText} has incorrect file size,\n" +
//                                               "the difference is ${sizeDiff?.absoluteValue ?: "unknown" } bytes.\n" +
//                                               "Check your internet connection and try again later")
//            }

//            val actualHashCode = runCatching { com.google.common.io.Files.asByteSource(downloadFile.toFile()).hash(
//                Hashing.sha256()).toString() }.getOrNull()
//            if (!actualHashCode.equals(item.sha256, ignoreCase = true)) {
//                throw RuntimeException("Failed to verify SHA-256 checksum for ${item.fullPresentationText}\n\n" +
//                                               "The actual value is ${actualHashCode ?: "unknown"},\n" +
//                                               "but expected ${item.sha256} was expected\n" +
//                                               "Check your internet connection and try again later")
//            }

            indicator.isIndeterminate = true
            indicator.text2 = "Unpacking"

            try {
//                if (wslDistribution != null) {
//                    JdkInstallerWSL.unpackJdkOnWsl(
//                        wslDistribution,
//                        item.packageType,
//                        downloadFile,
//                        targetDir,
//                        item.packageRootPrefix
//                    )
//                }
//                else {

                tempExtractionDir.toFile().mkdir()

                Decompressor.Zip(tempDownloadFile).withZipExtensions()
                    .entryFilter { indicator.checkCanceled(); true }
//                    .let {
//                        val fullMatchPath = item.packageRootPrefix.trim('/')
//                        if (fullMatchPath.isBlank()) it else it.removePrefixPath(fullMatchPath)
//                    }
                    .extract(tempExtractionDir)

//                runCatching { writeMarkerFile(item) }
//                JdkDownloaderLogger.logDownload(true)
            } catch (t: Throwable) {
                if (t is ControlFlowException) throw t
                throw RuntimeException("Failed to extract ${aptosItem.archiveNameNoExtension}. ${t.message}", t)
            }

            try {
                FileUtil.copy(tempExtractionDir.resolve("aptos").toFile(), targetFilePath.toFile())

            } catch (t: Throwable) {
                if (t is ControlFlowException) throw t
                throw RuntimeException("Failed to copy to ${targetFilePath}. ${t.message}", t)
            }

            onFinish(aptosItem)

        } catch (t: Throwable) {
            //if we were cancelled in the middle or failed, let's clean up
            JdkDownloaderLogger.logDownload(false)
//            targetDir.delete()
//            markerFile(targetDir)?.delete()
            throw t
        } finally {
            runCatching { FileUtil.delete(tempDownloadFile) }
            runCatching { FileUtil.delete(tempExtractionDir) }
        }
    }

    companion object {
        val installDir: Path get() = Paths.get(System.getProperty("user.home"), "aptos-cli")
    }
}

//class AptosDownloadTask(val aptosItem: AptosItem): SdkDownloadTask {
//    override fun getSuggestedSdkName(): String = "aptos"
//    override fun getPlannedHomeDir(): String = joinPath(arrayOf(System.getProperty("user.home"), "aptos-cli"))
//    override fun getPlannedVersion(): String = "3.1.0"
//
//    override fun doDownload(indicator: ProgressIndicator) {
//        service<AptosInstaller>().installAptosCli(aptosItem, indicator)
//    }
//}

//class AptosDownload: SdkDownload {
//    override fun supportsDownload(sdkTypeId: SdkTypeId): Boolean {
//        return true
//    }
//
//    override fun showDownloadUI(
//        sdkTypeId: SdkTypeId,
//        sdkModel: SdkModel,
//        parentComponent: JComponent,
//        selectedSdk: Sdk?,
//        sdkCreatedCallback: Consumer<in SdkDownloadTask>
//    ) {
//        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
//        val project = CommonDataKeys.PROJECT.getData(dataContext)
//        if (project?.isDisposed == true) return
//
//        TODO("Not yet implemented")
//    }
//}