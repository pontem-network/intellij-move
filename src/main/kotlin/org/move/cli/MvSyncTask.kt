@file:Suppress("UnstableApiUsage")

package org.move.cli

import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.SyncViewManager
import com.intellij.build.progress.BuildProgress
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

class MvSyncTask(
    project: Project,
    private val result: CompletableFuture<List<MoveProject>>
) : Task.Backgroundable(project, "Reloading Cargo projects", true) {

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        val syncProgress = SyncViewManager.createBuildProgress(project)
        try {
            syncProgress.start(createSyncProgressDescriptor(indicator))
            val projects = doSync(project)
            result.complete(projects)
            syncProgress.finish()
        } catch (e: Throwable) {
            if (e is ProcessCanceledException) {
                syncProgress.cancel()
            } else {
                syncProgress.fail()
            }
            result.completeExceptionally(e)
            throw e
        }
    }

    private fun createSyncProgressDescriptor(progress: ProgressIndicator): BuildProgressDescriptor {
        val buildContentDescriptor =
            BuildContentDescriptor(
                null, null, object : JComponent() {}, "Move"
            )
        buildContentDescriptor.isActivateToolWindowWhenFailed = true
        buildContentDescriptor.isActivateToolWindowWhenAdded = false
        val refreshAction = ActionManager.getInstance().getAction("Move.RefreshAllProjects")
        val descriptor =
            DefaultBuildDescriptor(Any(), "Move", project.basePath!!, System.currentTimeMillis())
                .withContentDescriptor { buildContentDescriptor }
                .withRestartAction(refreshAction)
                .withRestartAction(StopAction(progress))

        return object : BuildProgressDescriptor {
            override fun getTitle(): String = descriptor.title
            override fun getBuildDescriptor(): BuildDescriptor = descriptor
        }
    }

    private class StopAction(private val progress: ProgressIndicator) :
        DumbAwareAction({ "Stop" }, AllIcons.Actions.Suspend) {

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = progress.isRunning
        }

        override fun actionPerformed(e: AnActionEvent) {
            progress.cancel()
        }
    }

    companion object {
        fun doSync(project: Project): List<MoveProject> {
            val moveTomlFiles = findMoveTomlFiles(project)
            return moveTomlFiles
                .mapNotNull { initializeMoveProject(project, it) }
                .toList()
        }
    }
}
