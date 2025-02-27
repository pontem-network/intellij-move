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
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutput
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
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.move.cli.MoveProject.UpdateStatus
import org.move.cli.manifest.MoveToml
import org.move.cli.manifest.TomlDependency
import org.move.cli.manifest.TomlDependency.Git.Companion.moveHome
import org.move.cli.runConfigurations.aptos.AptosExitStatus
import org.move.cli.settings.getAptosCli
import org.move.cli.settings.moveSettings
import org.move.lang.toNioPathOrNull
import org.move.lang.toTomlFile
import org.move.openapiext.AnsiEscapedProcessAdapter
import org.move.openapiext.TaskResult
import org.move.openapiext.contentRoots
import org.move.openapiext.runProcessWithGlobalProgress
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
            contentRoot.iterateFiles({ it.name == MvConstants.MANIFEST_FILE }) { moveTomlFile ->
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
        val packageRoot = moveTomlFile.parent?.toNioPathOrNull() ?: error("cannot be invalid path")
        var (moveProject, rootMoveToml) =
            runReadAction {
                val tomlFile = moveTomlFile.toTomlFile(project)!!
                val rootMoveToml = MoveToml.fromTomlFile(tomlFile)
                val rootPackage = MovePackage.fromMoveToml(rootMoveToml, null)
                val rootProject = MoveProject(project, rootPackage, emptyList())
                rootProject to rootMoveToml
            }

        if (!project.moveSettings.fetchAptosDeps) {
            context.syncProgress.info(
                "Fetching dependencies (disabled)",
                "Fetching dependencies disabled in the plugin settings",
            )
        } else {
            context.runWithChildProgress("Fetch dependencies") { childContext ->
                val taskResult = fetchDependencyPackages(childContext, packageRoot)
                if (taskResult is TaskResult.Err) {
                    moveProject = moveProject.copy(
                        fetchDepsStatus = UpdateStatus.UpdateFailed("Failed to fetch dependency packages")
                    )
                }
                childContext.checkCanceled()
                // fetched any dependencies
                if (taskResult is TaskResult.Ok) {
                    val output = taskResult.value
                    if (output.stdout.contains("FETCHING GIT DEPENDENCY")) {
                        childContext.withProgressText("Preparing filesystem")
                        VfsUtil.markDirtyAndRefresh(
                            false,
                            true,
                            false,
                            moveHome().toFile()
                        )
                    }
                }
                taskResult
            }
        }
        context.checkCanceled()

        val deps =
            (context.runWithChildProgress("Load modules") { childContext ->
                // Blocks till completed or cancelled by the toml / file change
                runReadAction {
                    val rootPackage = moveProject.currentPackage
                    val deps = mutableListOf<MovePackageWithAddrSubst>()
                    val visitedDepIds = mutableSetOf(DepId(rootPackage.contentRoot.path))
                    loadDependencies(
                        project,
                        rootMoveToml,
                        deps,
                        visitedDepIds,
                        true,
                        childContext,
                        null,
                    )
                    TaskResult.Ok(deps)
                }
            } as TaskResult.Ok).value

        projects.add(moveProject.copy(dependencies = deps))
    }

    private fun fetchDependencyPackages(
        childContext: SyncContext, packageRoot: Path
    ): TaskResult<ProcessOutput> {
        val aptos = project.getAptosCli(parentDisposable = this)
        return when {
            aptos == null -> TaskResult.Err("Invalid Aptos CLI configuration")
            else -> {
                val aptosProcessOutput =
                    aptos.fetchPackageDependencies(
                        project,
                        packageRoot,
                        runner = {
                            // populate progress bar
                            addProcessListener(object: AnsiEscapedProcessAdapter() {
                                override fun onColoredTextAvailable(text: String) {
                                    // show progress bars for the long operations
                                    val line = text.trim()
                                    if (line.startsWith("FETCHING GIT DEPENDENCY")) {
                                        val gitRepo = line.substring("FETCHING GIT DEPENDENCY".length)
                                        childContext.withProgressText("Fetching $gitRepo")
                                    }
                                    if (line.startsWith("UPDATING GIT DEPENDENCY")) {
                                        val gitRepo = line.substring("UPDATING GIT DEPENDENCY".length)
                                        childContext.withProgressText("Updating $gitRepo")
                                    }
                                }
                            })
                            addProcessListener(object: ProcessListener {
                                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                                    childContext.syncProgress.output(event.text, true)
                                }
                            })
                            runProcessWithGlobalProgress()
                        }
                    ).unwrapOrElse {
                        return TaskResult.Err(
                            "Failed to fetch / update dependencies",
                            it.message
                        )
                    }
                val output = aptosProcessOutput.output
                when (val exitStatus = aptosProcessOutput.exitStatus) {
                    is AptosExitStatus.Result -> {
                        TaskResult.Ok(output)
                    }
                    is AptosExitStatus.Error -> {
                        if (exitStatus.message.contains("Unable to resolve packages for package")) {
                            return TaskResult.Err(
                                "Unable to resolve packages",
                                exitStatus.message.split(": ").joinToString(": \n")
                            )
                        }
                        if (!exitStatus.message.contains("Compilation error")) {
                            return TaskResult.Err(
                                "Error occurred",
                                exitStatus.message.split(": ").joinToString(": \n")
                            )
                        }
                        TaskResult.Ok(output)
                    }
                    is AptosExitStatus.Malformed -> TaskResult.Ok(output)
                }
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

        fun checkCanceled() = progress.checkCanceled()

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

        @RequiresReadLock
        private fun loadDependencies(
            project: Project,
            rootMoveToml: MoveToml,
            deps: MutableList<MovePackageWithAddrSubst>,
            visitedIds: MutableSet<DepId>,
            isRoot: Boolean,
            syncContext: SyncContext,
            gitRev: String?
        ) {
            // checks for the cancel() of the whole SyncTask
            syncContext.checkCanceled()

            var parsedDeps = rootMoveToml.deps
            if (isRoot) {
                parsedDeps = parsedDeps.withExtended(rootMoveToml.dev_deps)
            }
            for ((dep, addrSubst) in parsedDeps) {
                val depRoot = dep.rootDirectory()
                    .unwrapOrElse {
                        syncContext.syncProgress.message(
                            "Failed to load ${dep.name}",
                            "Error when resolving dependency root, \n${it.message}",
                            MessageEvent.Kind.ERROR,
                            null
                        )
                        null
                    } ?: continue

                val depId = DepId(depRoot.path)
                if (depId in visitedIds) continue

                val depTomlFile =
                    depRoot.findChild(MvConstants.MANIFEST_FILE)?.toTomlFile(project) ?: continue
                val depMoveToml = MoveToml.fromTomlFile(depTomlFile)

                // first try to parse MovePackage from dependency, no need for nested if parent is invalid
                val gitRev = (dep as? TomlDependency.Git)?.rev ?: gitRev
                val depPackage = MovePackage.fromMoveToml(depMoveToml, gitRev)

                // parse all nested dependencies with their address maps
                visitedIds.add(depId)
                loadDependencies(project, depMoveToml, deps, visitedIds, false, syncContext, gitRev)
                deps.add(MovePackageWithAddrSubst(depPackage, addrSubst))
//                deps.add(depPackage to addrSubst)

                syncContext.syncProgress.output(
                    "${dep.name} dependency loaded successfully, " +
                            "\npackage directory: \n${depRoot.presentableUrl}",
                    true,
                )
                syncContext.syncProgress.newline()
                syncContext.syncProgress.newline()
            }
        }
    }
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

private fun BuildProgress<BuildProgressDescriptor>.error(
    @BuildEventsNls.Title title: String,
    @BuildEventsNls.Message message: String
) {
    message(title, message, MessageEvent.Kind.ERROR, null)
}

private fun BuildProgress<BuildProgressDescriptor>.warning(
    @BuildEventsNls.Title title: String,
    @BuildEventsNls.Message message: String
) {
    message(title, message, MessageEvent.Kind.WARNING, null)
}

private fun BuildProgress<BuildProgressDescriptor>.info(
    @BuildEventsNls.Title title: String,
    @BuildEventsNls.Message message: String
) {
    message(title, message, MessageEvent.Kind.INFO, null)
}

private fun BuildProgress<BuildProgressDescriptor>.newline() {
    output("\n", true)
}


