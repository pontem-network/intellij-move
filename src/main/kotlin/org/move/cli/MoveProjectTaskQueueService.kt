package org.move.cli

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.BackgroundTaskQueue
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

@Service
class MvProjectTaskQueueService(val project: Project) {
    private val queue: BackgroundTaskQueue = BackgroundTaskQueue(project, "Move tasks queue")

    fun run(task: Task.Backgroundable) = queue.run(task)

    val isEmpty: Boolean get() = queue.isEmpty
}

val Project.taskQueue: MvProjectTaskQueueService get() = service()
