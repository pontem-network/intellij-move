package org.move.cli

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.move.utils.MvBackgroundTaskQueue

@Service(Service.Level.PROJECT)
class MvProjectTaskQueueService(val project: Project): Disposable {
    private val queue = MvBackgroundTaskQueue()

    fun run(task: Task.Backgroundable) = queue.run(task)

    val isEmpty: Boolean get() = queue.isEmpty

    override fun dispose() {
        queue.dispose()
    }
}

val Project.taskQueue: MvProjectTaskQueueService get() = service()
