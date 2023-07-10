package org.move.ide.actions.download

import java.io.File

sealed class DownloadStatus {
    class Downloading(val report: String) : DownloadStatus()

    class Finished(val destinationFile: String, query: String, destinationDir: File) : DownloadStatus()

    class Failed(val reason: String, query: String, destinationDir: File) : DownloadStatus() {
        override fun toString() = "Failed[$reason]"
    }
}

// :
//    DownloadStatus(query, destinationDir)

//class Finished(val destinationFile: String, query: String, destinationDir: File) : DownloadStatus(query, destinationDir)
//
//class Failed(val reason: String, query: String, destinationDir: File) : DownloadStatus(query, destinationDir) {

//}
