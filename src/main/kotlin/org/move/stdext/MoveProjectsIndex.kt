package org.move.stdext

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
import org.move.cli.MoveProject
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

sealed class IndexEntry {
    object Missing : IndexEntry()
    data class Present(val value: MoveProject?) : IndexEntry()
}

class MoveProjectsIndex(
    parentDisposable: Disposable,
    private val myInitializer: Consumer<MoveProjectsIndex>
) {
    private val entryMap: MutableMap<VirtualFile, IndexEntry> = ConcurrentHashMap()

    fun resetIndex() {
        entryMap.clear()
        myInitializer.accept(this)
    }

    fun put(file: VirtualFile?, value: IndexEntry) {
        if (file !is VirtualFileWithId) return
        entryMap[file] = value
    }

    fun get(file: VirtualFile): IndexEntry {
        if (file !is VirtualFileWithId || !file.isValid) return IndexEntry.Missing
        return entryMap.getOrDefault(file, IndexEntry.Missing)
    }

    companion object {
        private fun isDirectoryEvent(event: VFileEvent): Boolean {
            if (event is VFileCreateEvent) {
                return event.isDirectory
            }
            val file = event.file
            return file == null || file.isDirectory
        }
    }

    init {
        resetIndex()
        val connection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
        connection.subscribe(FileTypeManager.TOPIC, object : FileTypeListener {
            override fun fileTypesChanged(event: FileTypeEvent) {
                resetIndex()
            }
        })
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                for (event in events) {
                    if (isDirectoryEvent(event)) {
                        resetIndex()
                        break
                    }
                }
            }
        })
    }
}
