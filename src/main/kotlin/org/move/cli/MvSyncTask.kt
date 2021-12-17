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
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

class MvSyncTask(
    project: Project,
//    private val moveProjects: List<MvProject>,
    private val result: CompletableFuture<List<MoveProject>>
) : Task.Backgroundable(project, "Reloading Cargo projects", true) {

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        val syncProgress = SyncViewManager.createBuildProgress(project)
        try {
            syncProgress.start(createSyncProgressDescriptor(indicator))
            val projects = doRun(indicator, syncProgress)
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

//        val executable = MvExecutable(project)
//        val moveFiles = findMvFilesDeepestFirst(project).toList()
//        for ((i, moveFile) in moveFiles.withIndex()) {
//            val moveFileRoot = moveFile.toNioPathOrNull()?.parent ?: continue
//            executable.build(moveFileRoot)
//            indicator.fraction = (i.toDouble()) / moveFiles.size
//        }
//
//        val moveExecPath = project.moveExecPath ?: return
//        MvExecutable(project, moveExecPath).version()
    }

    private fun doRun(
        indicator: ProgressIndicator,
        syncProgress: BuildProgress<BuildProgressDescriptor>
    ): List<MoveProject> {
        val moveTomlFiles = findMoveTomlFiles(project)
        val moveProjects = moveTomlFiles.mapNotNull { initializeMoveProject(project, it) }
        return moveProjects.toList()
//        val refreshedProjects = if (project.)
//        val executable = project.moveExecutable
//        val refreshedProjects = if (executable == null) {
//            syncProgress.fail(System.currentTimeMillis(), "Mv project update failed:\nMv configured incorrectly")
//            moveProjects
//        } else {
//            moveProjects.map {
//
//            }
//        }
//        val moveTomlFiles = findMvFilesDeepestFirst(project).toList()
//        for ((i, moveTomlFile) in findMoveTomlFilesDeepestFirst(project).withIndex()) {
//            val moveTomlPath = moveTomlFile.toNioPathOrNull() ?: continue
//            val tomlFile = parseToml(project, moveTomlPath) ?: continue
//            val moveToml = MoveToml.fromTomlFile(tomlFile) ?: continue
//            val packageName = moveToml.packageTable?.name ?: "<unknown>"
//
//            // TODO: figure out how to check for success/failure of the build, if not then do not update the project
//            syncProgress.runWithChildProgress(
//                "Sync $packageName project",
//                createContext = { it },
//                action = { progress ->
//                    val (out, err) = executable.build(moveToml.root) ?: return@runWithChildProgress
//                    progress.message("Output", out, MessageEvent.Kind.ERROR, null)
//                    progress.message("Output", err, MessageEvent.Kind.ERROR, null)
//                })
////            indicator.fraction = (i.toDouble()) / moveTomlFiles.size
//        }
    }

    private fun createSyncProgressDescriptor(progress: ProgressIndicator): BuildProgressDescriptor {
        val buildContentDescriptor =
            BuildContentDescriptor(
                null, null, object : JComponent() {}, "Move"
            )
        buildContentDescriptor.isActivateToolWindowWhenFailed = true
        buildContentDescriptor.isActivateToolWindowWhenAdded = false
//        buildContentDescriptor.isNavigateToError = project.rustSettings.autoShowErrorsInEditor
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

    companion object {
        private val LOG = logger<MvSyncTask>()
    }

//    data class SyncContext(
//        val project: Project,
//        val progress: ProgressIndicator,
//        val syncProgress: BuildProgress<BuildProgressDescriptor>
//    ) {
//        fun <T> runWithChildProgress(
//            title: String,
//            action: (SyncContext) -> TaskResult<T>
//        ): TaskResult<T> {
//            progress.checkCanceled()
//            progress.text = title
//
//            return syncProgress.runWithChildProgress(title, { copy(syncProgress = it) }, action) { childProgress, result ->
//                when (result) {
//                    is TaskResult.Ok -> childProgress.finish()
//                    is TaskResult.Err -> {
//                        childProgress.message(result.reason, result.message.orEmpty(), MessageEvent.Kind.ERROR, null)
//                        childProgress.fail()
//                    }
//                }
//            }
//        }
//    }

    private class StopAction(private val progress: ProgressIndicator) :
        DumbAwareAction({ "Stop" }, AllIcons.Actions.Suspend) {

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = progress.isRunning
        }

        override fun actionPerformed(e: AnActionEvent) {
            progress.cancel()
        }
    }
}

private fun <T, R> BuildProgress<BuildProgressDescriptor>.runWithChildProgress(
    title: String,
    createContext: (BuildProgress<BuildProgressDescriptor>) -> T,
    action: (T) -> R,
    onResult: (BuildProgress<BuildProgressDescriptor>, R) -> Unit = { progress, _ -> progress.finish() }
): R {
    val childProgress = startChildProgress(title)
    try {
        val context = createContext(childProgress)
        val result = action(context)
        onResult(childProgress, result)
        return result
    } catch (e: Throwable) {
        if (e is ProcessCanceledException) {
            cancel()
        } else {
            fail()
        }
        throw e
    }
}
