@file:Suppress("UnstableApiUsage")

package org.move.cli

import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.SyncViewManager
import com.intellij.build.events.BuildEventsNls
import com.intellij.build.events.MessageEvent
import com.intellij.build.progress.BuildProgress
import com.intellij.build.progress.BuildProgressDescriptor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.MoveProject.UpdateStatus
import org.move.cli.manifest.MoveToml
import org.move.cli.settings.getAptosCli
import org.move.cli.settings.moveSettings
import org.move.lang.toNioPathOrNull
import org.move.lang.toTomlFile
import org.move.openapiext.TaskResult
import org.move.openapiext.contentRoots
import org.move.openapiext.resolveExisting
import org.move.openapiext.toVirtualFile
import org.move.stdext.iterateFiles
import org.move.stdext.unwrapOrElse
import org.move.stdext.withExtended
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

class MoveProjectsSyncTask(
    project: Project,
    parentDisposable: Disposable,
    private val future: CompletableFuture<List<MoveProject>>,
    private val reason: String?
): Task.Backgroundable(project, "Reloading Aptos packages", true), Disposable {

    init {
        Disposer.register(parentDisposable, this)
    }

    override fun dispose() {}

    override fun onCancel() {
        Disposer.dispose(this)
    }

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true

        val before = System.currentTimeMillis()
        LOG.logProjectsRefresh("started", reason)

        val syncProgress = SyncViewManager.createBuildProgress(project)

        val refreshedProjects = try {
            syncProgress.start(createSyncProgressDescriptor(indicator))

            val refreshedProjects = doRun(indicator, syncProgress)

            val isUpdateFailed =
                refreshedProjects.any { it.mergedStatus is UpdateStatus.UpdateFailed }
            if (isUpdateFailed) {
                syncProgress.fail()
            } else {
                syncProgress.finish()
            }
            refreshedProjects
        } catch (e: Throwable) {
            if (e is ProcessCanceledException) {
                syncProgress.cancel()
            } else {
                syncProgress.fail()
            }
            future.completeExceptionally(e)
            throw e
        }
        future.complete(refreshedProjects)

        val elapsed = System.currentTimeMillis() - before
        LOG.logProjectsRefresh("finished Aptos projects Sync Task in $elapsed ms", reason)

        Disposer.dispose(this)
    }

    private fun doRun(
        indicator: ProgressIndicator,
        syncProgress: BuildProgress<BuildProgressDescriptor>
    ): List<MoveProject> {
        val moveProjects = mutableListOf<MoveProject>()

        for (contentRoot in project.contentRoots) {
            contentRoot.iterateFiles({ it.name == Consts.MANIFEST_FILE }) { moveTomlFile ->
                indicator.checkCanceled()

                val projectDirName = moveTomlFile.parent.name
                syncProgress.runWithChildProgress(
                    "Sync $projectDirName project",
                    createContext = { it },
                    action = { childProgress ->
                        val context = SyncContext(project, indicator, syncProgress.id, childProgress)
                        loadProject(
                            moveTomlFile, projects = moveProjects, context = context
                        )
                    }
                )

                true
            }
        }

        return moveProjects
    }

    private fun loadProject(
        moveTomlFile: VirtualFile,
        projects: MutableList<MoveProject>,
        context: SyncContext
    ) {
        val projectRoot = moveTomlFile.parent?.toNioPathOrNull() ?: error("cannot be invalid path")
        var moveProject =
            runReadAction {
                val tomlFile = moveTomlFile.toTomlFile(project)!!
                val moveToml = MoveToml.fromTomlFile(tomlFile, projectRoot)
                val rootPackage = MovePackage.fromMoveToml(moveToml)
                MoveProject(project, rootPackage, emptyList())
            }

        val result = fetchDependencyPackages(context, projectRoot)
        if (result is TaskResult.Err) {
            moveProject =
                moveProject.copy(fetchDepsStatus = UpdateStatus.UpdateFailed("Failed to fetch dependency packages"))
        }

        val deps =
            (context.runWithChildProgress("Loading dependencies") { childContext ->
                // Blocks till completed or cancelled by the toml / file change
                runReadAction {
                    val rootPackage = moveProject.currentPackage
                    val deps = mutableListOf<Pair<MovePackage, RawAddressMap>>()
                    val visitedDepIds = mutableSetOf(DepId(rootPackage.contentRoot.path))
                    loadDependencies(
                        project,
                        rootPackage.moveToml,
                        deps,
                        visitedDepIds,
                        true,
                        childContext.progress
                    )
                    TaskResult.Ok(deps)
                }
            } as TaskResult.Ok).value

        projects.add(moveProject.copy(dependencies = deps))
    }

    private fun fetchDependencyPackages(context: SyncContext, projectRoot: Path): TaskResult<Unit> =
        context.runWithChildProgress("Synchronize dependencies") { childContext ->
            val listener = SyncProcessAdapter(childContext)

            val skipLatest = project.moveSettings.skipFetchLatestGitDeps
            val aptos = project.getAptosCli(parentDisposable = this)
            when {
                aptos == null -> TaskResult.Err("Invalid Aptos CLI configuration")
                else -> {
                    aptos.fetchPackageDependencies(
                        project,
                        projectRoot,
                        skipLatest,
                        processListener = listener
                    ).unwrapOrElse {
                        return@runWithChildProgress TaskResult.Err(
                            "Failed to fetch / update dependencies",
                            it.message
                        )
                    }
                    TaskResult.Ok(Unit)
                }
            }
        }

    private fun createSyncProgressDescriptor(progress: ProgressIndicator): BuildProgressDescriptor {
        val buildContentDescriptor = BuildContentDescriptor(
            null,
            null,
            object: JComponent() {},
            "Aptos"
        )
        buildContentDescriptor.isActivateToolWindowWhenFailed = true
        buildContentDescriptor.isActivateToolWindowWhenAdded = false
//        buildContentDescriptor.isNavigateToError = project.rustSettings.autoShowErrorsInEditor
        val refreshAction = ActionManager.getInstance().getAction("Move.RefreshAllProjects")
        val descriptor = DefaultBuildDescriptor(Any(), "Aptos", project.basePath!!, System.currentTimeMillis())
            .withContentDescriptor { buildContentDescriptor }
            .withRestartAction(refreshAction)
            .withRestartAction(StopAction(progress))
        return object: BuildProgressDescriptor {
            override fun getTitle(): String = descriptor.title
            override fun getBuildDescriptor(): BuildDescriptor = descriptor
        }
    }

    private class StopAction(private val progress: ProgressIndicator):
        DumbAwareAction({ "Stop" }, AllIcons.Actions.Suspend) {

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = progress.isRunning
        }

        override fun actionPerformed(e: AnActionEvent) = progress.cancel()

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }

    data class SyncContext(
        val project: Project,
//        val toolchain: RsToolchainBase,
        val progress: ProgressIndicator,
        val buildId: Any,
        val syncProgress: BuildProgress<BuildProgressDescriptor>
    ) {

        val id: Any get() = syncProgress.id

        fun <T> runWithChildProgress(
            @NlsContexts.ProgressText title: String,
            action: (SyncContext) -> TaskResult<T>
        ): TaskResult<T> {
            progress.checkCanceled()
            progress.text = title

            return syncProgress.runWithChildProgress(
                title,
                { copy(syncProgress = it) },
                action
            ) { childProgress, result ->
                when (result) {
                    is TaskResult.Ok -> childProgress.finish()
                    is TaskResult.Err -> {
                        childProgress.message(
                            result.reason,
                            result.message.orEmpty(),
                            MessageEvent.Kind.ERROR,
                            null
                        )
                        childProgress.fail()
                    }
                }
            }
        }

        fun withProgressText(@NlsContexts.ProgressText @NlsContexts.ProgressTitle text: String) {
            progress.text = text
            syncProgress.progress(text)
        }
    }

    companion object {
        private val LOG = logger<MoveProjectsSyncTask>()

        private data class DepId(val rootPath: String)

        private fun loadDependencies(
            project: Project,
            rootMoveToml: MoveToml,
            deps: MutableList<Pair<MovePackage, RawAddressMap>>,
            visitedIds: MutableSet<DepId>,
            isRoot: Boolean,
            progress: ProgressIndicator
        ) {
            // checks for the cancel() of the whole SyncTask
            progress.checkCanceled()

            var parsedDeps = rootMoveToml.deps
            if (isRoot) {
                parsedDeps = parsedDeps.withExtended(rootMoveToml.dev_deps)
            }
            for ((dep, addressMap) in parsedDeps) {
                val depRoot = dep.localPath()

                val depId = DepId(depRoot.toString())
                if (depId in visitedIds) continue

                val depTomlFile = depRoot
                    .resolveExisting(Consts.MANIFEST_FILE)
                    ?.toVirtualFile()
                    ?.toTomlFile(project) ?: continue
                val depMoveToml = MoveToml.fromTomlFile(depTomlFile, depRoot)

                // first try to parse MovePackage from dependency, no need for nested if parent is invalid
                val depPackage = MovePackage.fromMoveToml(depMoveToml)

                // parse all nested dependencies with their address maps
                visitedIds.add(depId)
                loadDependencies(project, depMoveToml, deps, visitedIds, false, progress)

                deps.add(Pair(depPackage, addressMap))
            }
        }
    }
}

private class SyncProcessAdapter(
    private val context: MoveProjectsSyncTask.SyncContext
): ProcessAdapter(),
   ProcessProgressListener {
    override fun onTextAvailable(event: ProcessEvent, outputType: Key<Any>) {
        val text = event.text.trim { it <= ' ' }
        if (text.startsWith("FETCHING GIT DEPENDENCY")) {
            val gitRepo = text.substring("FETCHING GIT DEPENDENCY".length)
            context.withProgressText("Fetching $gitRepo")
        }
        if (text.startsWith("UPDATING GIT DEPENDENCY")) {
            val gitRepo = text.substring("UPDATING GIT DEPENDENCY".length)
            context.withProgressText("Updating $gitRepo")
        }
    }

    override fun error(title: String, message: String) = context.error(title, message)
    override fun warning(title: String, message: String) = context.warning(title, message)
}

private fun <T, R> BuildProgress<BuildProgressDescriptor>.runWithChildProgress(
    @BuildEventsNls.Title title: String,
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

private fun MoveProjectsSyncTask.SyncContext.error(
    @BuildEventsNls.Title title: String,
    @BuildEventsNls.Message message: String
) {
    syncProgress.message(title, message, com.intellij.build.events.MessageEvent.Kind.ERROR, null)
}

private fun MoveProjectsSyncTask.SyncContext.warning(
    @BuildEventsNls.Title title: String,
    @BuildEventsNls.Message message: String
) {
    syncProgress.message(title, message, com.intellij.build.events.MessageEvent.Kind.WARNING, null)
}


