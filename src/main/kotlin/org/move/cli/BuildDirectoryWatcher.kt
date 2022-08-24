package org.move.cli

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class BuildDirectoryWatcher(
    private var moveProjects: List<MoveProject>,
    private val onChange: () -> Unit
) : BulkFileListener {

    fun updateProjects(moveProjects: List<MoveProject>) {
        this.moveProjects = moveProjects
    }

    override fun after(events: MutableList<out VFileEvent>) {
        val buildDirectories = moveProjects.map { FileUtil.join(it.contentRoot.path, "build") }
        for (event in events) {
            if (buildDirectories.any { event.pathStartsWith(it) }) {
                onChange()
                return
            }
        }
    }
}
