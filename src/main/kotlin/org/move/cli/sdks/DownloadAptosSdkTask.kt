package org.move.cli.sdks

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.component1
import com.intellij.openapi.util.component2
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.Decompressor
import java.io.File
import java.io.IOException
import java.nio.file.Path

class DownloadAptosSdkTask(
    private val aptosSdk: AptosSdk,
    private val onFinish: (AptosSdk) -> Unit
):
    Task.Modal(null, "Aptos SDK Installer", true) {

    override fun run(indicator: ProgressIndicator) {
        indicator.text = "Installing Aptos SDK v${aptosSdk.version}..."

        indicator.text2 = "Fetching ${aptosSdk.githubArchiveUrl}"
        val tmpDownloadDir = File(FileUtil.getTempDirectory(), "aptos-clis")

        val archiveFileName = aptosSdk.githubArchiveFileName
        val tmpExtractionDir =
            tmpDownloadDir.resolve(
                FileUtil.getNameWithoutExtension(aptosSdk.githubArchiveFileName)
            )

        val url = aptosSdk.githubArchiveUrl
        try {
            val tmpDownloadFilePath: Path
            try {
                val downloadService = DownloadableFileService.getInstance()
                val downloader = downloadService.createDownloader(
                    listOf(
                        downloadService.createFileDescription(url, aptosSdk.githubArchiveFileName)
                    ),
                    "Download Aptos SDK"
                )
                val (file, _) = downloader.download(tmpDownloadDir).first()
                tmpDownloadFilePath = file.toPath()
            } catch (e: IOException) {
                throw RuntimeException(
                    "Failed to download $archiveFileName from $url. ${e.message}",
                    e
                )
            }

            indicator.isIndeterminate = true
            indicator.text = "Installing Aptos SDK..."

            indicator.text2 = "Unpacking $archiveFileName"
            try {
                tmpExtractionDir.mkdir()
                Decompressor.Zip(tmpDownloadFilePath).withZipExtensions()
                    .entryFilter { indicator.checkCanceled(); true }
                    .extract(tmpExtractionDir.toPath())
            } catch (t: Throwable) {
                if (t is ControlFlowException) throw t
                throw RuntimeException(
                    "Failed to extract $tmpDownloadFilePath. ${t.message}",
                    t
                )
            }

            val extractedFile = tmpExtractionDir.resolve("aptos")
            try {
                FileUtil.copy(extractedFile, aptosSdk.targetFile)
            } catch (t: Throwable) {
                if (t is ControlFlowException) throw t
                throw RuntimeException(
                    "Failed to copy from $extractedFile to ${aptosSdk.targetFile}. ${t.message}",
                    t
                )
            }

            onFinish(aptosSdk)

        } catch (t: Throwable) {
            //if we were cancelled in the middle or failed, let's clean up
            // todo: log download somehow
//            JdkDownloaderLogger.logDownload(false)
            throw t
        } finally {
            runCatching { FileUtil.delete(tmpExtractionDir) }
        }
    }
}
