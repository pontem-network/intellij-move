package org.move.ide.actions.download

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import javax.swing.JComponent
import javax.swing.SwingUtilities

class DownloadAptosTask(
    parentComponent: JComponent,
    private val aptosVersion: String,
    private val destDir: String,
) :
    Task.WithResult<String?, Exception>(null, parentComponent, "Downloading Aptos CLI", true) {

    @Suppress("PrivatePropertyName")
    private val LOG = logger<DownloadAptosTask>()

    override fun compute(indicator: ProgressIndicator): String? {
        val os = os() ?: return null
        val downloadUrl = "https://github.com/aptos-labs/aptos-core" +
                "/releases/download/aptos-cli-v$aptosVersion/aptos-cli-$aptosVersion-$os-x86_64.zip"

        LOG.debug("Downloading '$downloadUrl' to '$destDir'")

        return download(downloadUrl, File(destDir)) {
            when (it) {
                is DownloadStatus.Downloading ->
                    SwingUtilities.invokeLater {
                        indicator.text = it.report
                    }

                is DownloadStatus.Failed -> {
                    // TODO: show error
                }

                is DownloadStatus.Finished -> {
                    // TODO: show report
                }
            }
        }
    }

    private fun download(
        aptosUrl: String,
        destinationDir: File,
        statusListener: (DownloadStatus) -> Unit
    ): String? {
        if (!destinationDir.isDirectory) {
            statusListener(
                DownloadStatus.Failed(
                    "${destinationDir.absolutePath} is not a directory",
                    aptosUrl,
                    destinationDir
                )
            )
            return null
        }
        if (aptosUrl.startsWith("http")) {
            val url = try {
                URL(aptosUrl)
            } catch (e: MalformedURLException) {
                statusListener(DownloadStatus.Failed("Bad Aptos HTTP url", aptosUrl, destinationDir))
                return null
            }
            val destinationFile = File(destinationDir, aptosUrl.toFileName())

            val httpConn = url.openConnection() as HttpURLConnection
            try {
                httpConn.getHeaderField("Content-Type")
            } catch (e: IllegalStateException) {
                statusListener(DownloadStatus.Failed("Nothing to download", aptosUrl, destinationDir))
                return null
            }

//            val fraction = 0.0
            statusListener(DownloadStatus.Downloading("Fetching $aptosUrl"))

            val inputStream = try {
                url.openStream()
            } catch (e: IOException) {
                statusListener(DownloadStatus.Failed("Can't connect", aptosUrl, destinationDir))
                return null
            }

            Files.copy(inputStream, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            // unzip
            val outFile = File(destinationDir, "aptos-$aptosVersion")
            statusListener(
                DownloadStatus.Downloading(
                    "Unzipping ${destinationFile.absoluteFile} to the ${outFile.name}"
                )
            )
            ZipFile(destinationFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        if (entry.name == "aptos") {
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                            outFile.setExecutable(true)
                        }
                    }
                }
            }
            // remove zip archive
            statusListener(DownloadStatus.Downloading("Removing zip archive"))
            destinationFile.delete()

            statusListener(DownloadStatus.Finished(outFile.absolutePath, aptosUrl, destinationDir))
            return outFile.absolutePath
        } else {
            statusListener(DownloadStatus.Failed("Can't understand link type", aptosUrl, destinationDir))
            return null
        }
    }

    private fun String.toFileName(): String {
        return if (this.lastIndexOf("/") != this.length && "/" in this) {
            URLEncoder.encode(this.substringAfterLast("/"), Charsets.UTF_8.toString())
        } else {
            URLEncoder.encode(this, Charsets.UTF_8.toString())
        }
    }

    private fun os(): String? {
        return when {
            SystemInfo.isWindows -> "Windows"
            SystemInfo.isLinux -> "Ubuntu"
            SystemInfo.isMac -> "MacOSX"
            else -> {
                // TODO: show error somewhere
                return null
            }
        }
    }
}

//class AptosDownloader(
//    private val parentComponent: JComponent,
//    private val aptosVersion: String,
//    private val destDir: String,
//) {
//    private val LOG = logger<AptosDownloader>()
//
//
//    fun start() {
//        object : Task.Modal(null, parentComponent, "Downloading Aptos CLI", true) {
//            override fun run(indicator: ProgressIndicator) {
//
//                val aptosUrl = aptosUrl() ?: return
//                LOG.debug("Downloading '$aptosUrl' to '$destDir'")
//
//                download(aptosUrl, File(destDir)) {
//                    when (it) {
//                        is DownloadStatus.Downloading ->
//                            SwingUtilities.invokeLater {
//                                indicator.text = it.report
//                            }
//
//                        is DownloadStatus.Failed -> {
//                            // TODO: show error
//                        }
//
//                        is DownloadStatus.Finished -> {
//                            // TODO: show report
//                        }
//                    }
//                }
//            }
//        }.queue()
//    }
//}
