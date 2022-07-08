package org.move.cli

import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

class MoveTomlWatcher(
    private val onMoveTomlChange: () -> Unit
) : BulkFileListener {

    override fun after(events: List<VFileEvent>) {
        if (events.any { isInterestingEvent(it) }) onMoveTomlChange()
    }

    private fun isInterestingEvent(event: VFileEvent): Boolean {
        return event.pathEndsWith(Consts.MANIFEST_FILE)
    }
}

fun VFileEvent.pathStartsWith(prefix: String): Boolean = path.startsWith(prefix) ||
        this is VFilePropertyChangeEvent && oldPath.startsWith(prefix)

fun VFileEvent.pathEndsWith(suffix: String): Boolean = path.endsWith(suffix) ||
        this is VFilePropertyChangeEvent && oldPath.endsWith(suffix)
