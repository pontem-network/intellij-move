package org.move.ide.actions.download

import java.io.File

sealed class DownloadStatus(val query: String, val destinationDir: File)

class Downloading(val fraction: Double, query: String, destinationDir: File) :
    DownloadStatus(query, destinationDir)

class Finished(val destinationFile: String, query: String, destinationDir: File) : DownloadStatus(query, destinationDir)

class Failed(val reason: String, query: String, destinationDir: File) : DownloadStatus(query, destinationDir) {

    override fun toString() = "Failed[$reason]"
}
