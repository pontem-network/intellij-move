package org.move.ide.actions.download

sealed class DownloadStatus {
    class Downloading(val report: String) : DownloadStatus()

    object Finished : DownloadStatus()

    class Failed(val reason: String) : DownloadStatus() {
        override fun toString() = "Failed[$reason]"
    }
}