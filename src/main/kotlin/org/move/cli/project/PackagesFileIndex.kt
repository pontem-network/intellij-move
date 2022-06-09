package org.move.cli.project

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.FileTypeEvent
import com.intellij.openapi.fileTypes.FileTypeListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.util.concurrent.ConcurrentHashMap

sealed class PackageFileIndexEntry {
    object Missing : PackageFileIndexEntry()
    data class Present(val package_: MovePackage?) : PackageFileIndexEntry()
}

class PackagesFileIndex(parentDisposable: Disposable) {

    private val entries: MutableMap<VirtualFile, PackageFileIndexEntry> = ConcurrentHashMap()

    init {
        reset()
        val connection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        connection.subscribe(FileTypeManager.TOPIC, object : FileTypeListener {
            override fun fileTypesChanged(event: FileTypeEvent) {
                reset()
            }
        })
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                for (event in events) {
                    if (isDirectoryCreatedEvent(event)) {
                        reset()
                        break
                    }
                }
            }
        })
    }

    fun reset() {
        entries.clear()
//        myInitializer.accept(this)
    }

    fun put(file: VirtualFile, value: PackageFileIndexEntry) {
        if (file !is VirtualFileWithId) return
        entries[file] = value
    }

    fun get(file: VirtualFile): PackageFileIndexEntry {
        if (file !is VirtualFileWithId || !file.isValid) return PackageFileIndexEntry.Missing
        return entries.getOrDefault(file, PackageFileIndexEntry.Missing)
    }

    companion object {
        private fun isDirectoryCreatedEvent(event: VFileEvent): Boolean {
            if (event is VFileCreateEvent) {
                return event.isDirectory
            }
            val file = event.file
            return file == null || file.isDirectory
        }
    }
}
