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
import com.intellij.util.Consumer
import org.move.cli.MoveProject
import java.util.concurrent.ConcurrentHashMap

sealed class MoveProjectEntry {
    object Missing : MoveProjectEntry()
    data class Present(val project: MoveProject?): MoveProjectEntry()
}
/**
 * This is a light version of DirectoryIndexImpl
 *
 * @author gregsh
 */
class MyLightDirectoryIndex<T>(
    parentDisposable: Disposable,
    private val myDefValue: T,
    private val myInitializer: Consumer<MyLightDirectoryIndex<T>>
) {
    private val myRootInfos: MutableMap<VirtualFile, T> = ConcurrentHashMap()
    fun resetIndex() {
        myRootInfos.clear()
        myInitializer.consume(this)
    }

    fun putInfo(file: VirtualFile?, value: T) {
        if (file !is VirtualFileWithId) return
        myRootInfos[file] = value
    }

    fun getInfoForFile(file: VirtualFile): T {
        if (file !is VirtualFileWithId || !file.isValid) return myDefValue
        return myRootInfos.getOrDefault(file, myDefValue)
    }

    companion object {
        private fun shouldReset(event: VFileEvent): Boolean {
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
                    if (shouldReset(event)) {
                        resetIndex()
                        break
                    }
                }
            }
        })
    }
}
