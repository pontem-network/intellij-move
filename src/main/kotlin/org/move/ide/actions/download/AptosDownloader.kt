package org.move.ide.actions.download

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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
import javax.swing.SwingUtilities

class AptosDownloader(
    private val project: Project,
    private val aptosVersion: String,
    private val destDir: String,
) {
    private val LOG = logger<AptosDownloader>()

    fun aptosUrl(): String? {
        val os = os() ?: return null
        return "https://github.com/aptos-labs/aptos-core" +
                "/releases/download/aptos-cli-v$aptosVersion/aptos-cli-$aptosVersion-$os-x86_64.zip"
    }

    fun start() {
        object : Task.Backgroundable(project, "Downloading Aptos CLI") {
            override fun run(indicator: ProgressIndicator) {

                val aptosUrl = aptosUrl() ?: return
                LOG.debug("Downloading '$aptosUrl' to '$destDir'")

                download(aptosUrl, File(destDir)) {
                    when (it) {
                        is Downloading -> SwingUtilities.invokeLater {
                            indicator.fraction = it.fraction
                        }

                        is Failed -> SwingUtilities.invokeLater {
                            Messages.showErrorDialog(
                                project,
                                "${it.reason}: $aptosUrl",
                                "Can't Download Link"
                            )
                        }

                        is Finished -> DownloadFinishNotifier.notify(
                            project = project,
                            title = "Download finished",
                            content = it.destinationFile
                        )
                    }
                }
            }
        }.queue()
    }

    private fun download(aptosUrl: String, destinationDir: File, statusListener: (DownloadStatus) -> Unit) {
        if (!destinationDir.isDirectory) {
            statusListener(
                Failed(
                    "${destinationDir.absolutePath} is not a directory",
                    aptosUrl,
                    destinationDir
                )
            )
            return
        }
        if (aptosUrl.startsWith("http")) {
            val url = try {
                URL(aptosUrl)
            } catch (e: MalformedURLException) {
                statusListener(Failed("Bad Aptos HTTP url", aptosUrl, destinationDir))
                return
            }
            val destinationFile = File(destinationDir, aptosUrl.toFileName())

            val httpConn = url.openConnection() as HttpURLConnection
            try {
                httpConn.getHeaderField("Content-Type")
            } catch (e: IllegalStateException) {
                statusListener(Failed("Nothing to download", aptosUrl, destinationDir))
                return
            }

            val inputStream = try {
                url.openStream()
            } catch (e: IOException) {
                statusListener(Failed("Can't connect", aptosUrl, destinationDir))
                return
            }
            Files.copy(inputStream, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            val outFile = File(destinationDir, "aptos-$aptosVersion")
            // unzip
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
            destinationFile.delete()

            statusListener(Finished(outFile.absolutePath, aptosUrl, destinationDir))
        } else {
            statusListener(Failed("Can't understand link type", aptosUrl, destinationDir))
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
                SwingUtilities.invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "OS unsupported by the Aptos",
                        "Unsupported OS",
                    )
                }
                return null
            }
        }
    }
}
