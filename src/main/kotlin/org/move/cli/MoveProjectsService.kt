package org.move.cli

import com.intellij.execution.RunManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.util.parents
import com.intellij.util.messages.Topic
import org.move.cli.settings.MvProjectSettingsServiceBase.*
import org.move.cli.settings.MvProjectSettingsServiceBase.Companion.MOVE_SETTINGS_TOPIC
import org.move.cli.settings.debugErrorOrFallback
import org.move.lang.core.psi.ext.elementType
import org.move.lang.toNioPathOrNull
import org.move.openapiext.checkReadAccessAllowed
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.debugInProduction
import org.move.openapiext.toVirtualFile
import org.move.stdext.AsyncValue
import org.move.stdext.CacheEntry
import org.move.stdext.FileToMoveProjectCache
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

val Project.moveProjectsService get() = service<MoveProjectsService>()

val Project.hasMoveProject get() = this.moveProjectsService.allProjects.isNotEmpty()

class MoveProjectsService(val project: Project): Disposable {

    private val refreshOnBuildDirChangeWatcher =
        BuildDirectoryWatcher(emptyList()) { scheduleProjectsRefresh("build/ directory changed") }

    var initialized = false

    init {
        with(project.messageBus.connect()) {
            if (!isUnitTestMode) {
                subscribe(VirtualFileManager.VFS_CHANGES, refreshOnBuildDirChangeWatcher)
                subscribe(VirtualFileManager.VFS_CHANGES, MoveTomlWatcher {
                    // on every Move.toml change
                    // TODO: move to External system integration
                    scheduleProjectsRefresh("Move.toml changed")
                })
            }
            subscribe(
                MOVE_SETTINGS_TOPIC,
                object: MoveSettingsListener {
                    override fun <T: MvProjectSettingsBase<T>> settingsChanged(e: SettingsChangedEventBase<T>) {
                        // on every Move Language plugin settings change
                        scheduleProjectsRefresh("plugin settings changed")
                    }
                })
        }
    }

    val allProjects: List<MoveProject>
        get() = this.projects.state

    fun scheduleProjectsRefresh(reason: String? = null): CompletableFuture<List<MoveProject>> {
        LOG.logProjectsRefresh("scheduled", reason)
        val moveProjectsFut =
            modifyProjectModel {
                doRefreshProjects(project, reason)
            }
        return moveProjectsFut
    }

    // requires ReadAccess
    fun findMoveProjectForPsiElement(psiElement: PsiElement): MoveProject? {
        // read access required for the psiElement.containingFile
        checkReadAccessAllowed()
        val file = when (psiElement) {
            is PsiDirectory -> psiElement.virtualFile
            is PsiFile -> psiElement.originalFile.virtualFile
            else -> {
                val containingFile =
                    try {
                        psiElement.containingFile
                    } catch (e: PsiInvalidElementAccessException) {
                        val parentsChain =
                            psiElement.parents(true).map { it.elementType }.joinToString(" -> ")
                        project.debugErrorOrFallback(
                            "Cannot get the containing file for the ${psiElement.javaClass.name}, " +
                                    "elementType is ${psiElement.elementType}, parents chain is $parentsChain",
                            cause = e
                        ) {
                            try {
                                psiElement.node.psi.containingFile
                            } catch (e: PsiInvalidElementAccessException) {
                                null
                            }
                        }
                    }
                containingFile?.originalFile?.virtualFile
            }
        } ?: return null
        return findMoveProjectForFile(file)
    }

    fun findMoveProjectForPath(path: Path): MoveProject? {
        val file = path.toVirtualFile() ?: return null
        return findMoveProjectForFile(file)
    }

    private fun doRefreshProjects(project: Project, reason: String?): CompletableFuture<List<MoveProject>> {
        val moveProjectsFut = CompletableFuture<List<MoveProject>>()
        val syncTask = MoveProjectsSyncTask(project, moveProjectsFut, reason)
        project.taskQueue.run(syncTask)
        return moveProjectsFut.thenApply { updatedProjects ->
            runOnlyInNonLightProject(project) {
                setupProjectRoots(project, updatedProjects)
            }
            updatedProjects
        }
    }

    fun findMoveProjectForFile(file: VirtualFile): MoveProject? {
        val cached = this.fileToMoveProjectCache.get(file)
        if (cached is CacheEntry.Present) return cached.value

        if (isUnitTestMode && file.fileSystem is TempFileSystem) return MoveProject.forTests(project)

        val filePath = file.toNioPathOrNull() ?: return null

        var resProject: MoveProject? = null
        val depthSortedProjects = this.projects.state.sortedByDescending { it.contentRoot.fsDepth }
        // first go through all the root packages of the projects
        for (candidate in depthSortedProjects) {
            val candidateRoot = candidate.currentPackage.contentRoot.toNioPathOrNull() ?: continue
            if (filePath.startsWith(candidateRoot)) {
                resProject = candidate
                break
            }
        }
        // go through dependencies
        for (candidate in this.projects.state) {
            if (resProject != null) break
            for (depPackage in candidate.depPackages()) {
                val depRoot = depPackage.contentRoot.toNioPathOrNull() ?: continue
                if (filePath.startsWith(depRoot)) {
                    resProject = candidate
                    break
                }
            }
        }
        this.fileToMoveProjectCache.put(file, CacheEntry.Present(resProject))
        return resProject
    }

    /**
     * The heart of the plugin Project model. Care must be taken to ensure
     * this is thread-safe, and that refreshes are scheduled after
     * set of projects changes.
     */
    private val projects = AsyncValue<List<MoveProject>>(emptyList())

    private val fileToMoveProjectCache = FileToMoveProjectCache(this)

    /**
     * All modifications to project model except for low-level `loadState` should
     * go through this method: it makes sure that when we update various IDEA listeners.
     */
    private fun modifyProjectModel(
        updater: (List<MoveProject>) -> CompletableFuture<List<MoveProject>>
    ): CompletableFuture<List<MoveProject>> {
        val wrappedUpdater = { projects: List<MoveProject> ->
            updater(projects)
        }
        return projects.updateAsync(wrappedUpdater)
            .thenApply { projects ->
                refreshOnBuildDirChangeWatcher.setWatchedProjects(projects)
                invokeAndWaitIfNeeded {
                    runWriteAction {
                        // remove file -> moveproject associations from cache
                        fileToMoveProjectCache.clear()

                        // disable for unit-tests: in those cases roots change is done by the test framework
                        runOnlyInNonLightProject(project) {
                            ProjectRootManagerEx.getInstanceEx(project)
                                .makeRootsChange(EmptyRunnable.getInstance(), false, true)
//                                .makeRootsChange(
//                                    EmptyRunnable.getInstance(),
//                                    RootsChangeRescanningInfo.TOTAL_RESCAN
//                                )
                        }
                        // increments structure modification counter in the subscriber
                        project.messageBus
                            .syncPublisher(MOVE_PROJECTS_TOPIC).moveProjectsUpdated(this, projects)
                        initialized = true
                    }
                }
                projects
            }
    }

    override fun dispose() {}

    companion object {
        private val LOG = logger<MoveProjectsService>()

        val MOVE_PROJECTS_TOPIC: Topic<MoveProjectsListener> = Topic.create(
            "move projects changes",
            MoveProjectsListener::class.java
        )
    }

    fun interface MoveProjectsListener {
        fun moveProjectsUpdated(service: MoveProjectsService, projects: Collection<MoveProject>)
    }
}

fun setupProjectRoots(project: Project, moveProjects: List<MoveProject>) {
    invokeAndWaitIfNeeded {
        // Initialize services that we use (probably indirectly) in write action below.
        // Otherwise, they can be initialized in write action that may lead to deadlock
        RunManager.getInstance(project)
        ProjectFileIndex.getInstance(project)

        runWriteAction {
            if (project.isDisposed) return@runWriteAction
            ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring {
                for (moveProject in moveProjects) {
                    val projectRoot = moveProject.contentRoot
                    val packageModule =
                        ModuleUtilCore.findModuleForFile(projectRoot, project) ?: continue
                    ModuleRootModificationUtil.updateModel(packageModule) { rootModel ->
                        val contentEntry = rootModel.contentEntries.singleOrNull() ?: return@updateModel
                        contentEntry.addExcludeFolder(FileUtil.join(projectRoot.url, "build"))
                    }
                }
            }
        }
    }
}

inline fun runOnlyInNonLightProject(project: Project, action: () -> Unit) {
    if ((project as? ProjectEx)?.isLight != true) {
        action()
    } else {
        check(isUnitTestMode)
    }
}

val VirtualFile.fsDepth: Int get() = this.path.split(File.separator).count()

fun Logger.logProjectsRefresh(status: String, reason: String? = null) {
    var logMessage = "PROJ_REFRESH $status"
    if (reason != null) {
        logMessage += " [$reason]"
    }
    this.debugInProduction(logMessage)
}
