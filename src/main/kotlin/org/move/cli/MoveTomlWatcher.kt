package org.move.cli

import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

class MoveTomlWatcher(
    private val onMoveTomlChange: () -> Unit
) : BulkFileListener {

    override fun before(events: List<VFileEvent>) = Unit

    override fun after(events: List<VFileEvent>) {
        if (events.any { isInterestingEvent(it) }) onMoveTomlChange()
    }

    private fun isInterestingEvent(event: VFileEvent): Boolean {
        return event.pathEndsWith(MoveConstants.MANIFEST_FILE)
    }

    private fun VFileEvent.pathEndsWith(suffix: String): Boolean = path.endsWith(suffix) ||
            this is VFilePropertyChangeEvent && oldPath.endsWith(suffix)
}
