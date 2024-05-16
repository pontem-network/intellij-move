package org.move.cli

import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

class OnMoveTomlCreatedFileListener(
    private val onMoveTomlAdded: () -> Unit
) : BulkFileListener {

    override fun after(events: List<VFileEvent>) {
        if (events.any { isInterestingEvent(it) }) onMoveTomlAdded()
    }

    private fun isInterestingEvent(event: VFileEvent): Boolean {
        return event is VFileCreateEvent && event.path.endsWith(Consts.MANIFEST_FILE)
                || event is VFilePropertyChangeEvent && event.newPath.endsWith(Consts.MANIFEST_FILE)
    }
}

fun VFileEvent.pathStartsWith(prefix: String): Boolean =
    path.startsWith(prefix) ||
            this is VFilePropertyChangeEvent && oldPath.startsWith(prefix)

fun VFileEvent.pathEndsWith(suffix: String): Boolean =
    path.endsWith(suffix) ||
            this is VFilePropertyChangeEvent && oldPath.endsWith(suffix)
