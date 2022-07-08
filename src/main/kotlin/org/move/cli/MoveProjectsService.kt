package org.move.cli

import com.intellij.execution.RunManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
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
import com.intellij.psi.PsiManager
import com.intellij.util.messages.Topic
import org.move.cli.settings.MoveProjectSettingsService
import org.move.cli.settings.MoveSettingsChangedEvent
import org.move.cli.settings.MoveSettingsListener
import org.move.ide.inspections.imports.MoveElementsIndex
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.toVirtualFile
import org.move.stdext.AsyncValue
import org.move.stdext.IndexEntry
import org.move.stdext.MoveProjectsIndex
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

val Project.moveProjects get() = service<MoveProjectsService>()

val Project.hasMoveProject get() = this.moveProjects.allProjects.isNotEmpty()

class MoveProjectsService(val project: Project) : Disposable {
    private val buildWatcher = BuildDirectoryWatcher(emptyList()) { refreshAllProjects() }

    init {
        with(project.messageBus.connect()) {
            if (!isUnitTestMode) {
                subscribe(VirtualFileManager.VFS_CHANGES, buildWatcher)
                subscribe(VirtualFileManager.VFS_CHANGES, MoveTomlWatcher {
                    refreshAllProjects()
                })
            }
            subscribe(MoveProjectSettingsService.MOVE_SETTINGS_TOPIC, object : MoveSettingsListener {
                override fun moveSettingsChanged(e: MoveSettingsChangedEvent) {
                    refreshAllProjects()
                }
            })
        }
    }

    fun refreshAllProjects() {
        LOG.info("Project state refresh started")
        modifyProjects { doRefresh(project) }
    }

    fun findMoveProject(psiElement: PsiElement): MoveProject? {
        val file = when (psiElement) {
            is PsiDirectory -> psiElement.virtualFile
            is PsiFile -> psiElement.originalFile.virtualFile
            else -> psiElement.containingFile?.originalFile?.virtualFile
        } ?: return null
        return findMoveProject(file)
    }

    fun findMoveProject(path: Path): MoveProject? {
        val file = path.toVirtualFile() ?: return null
        return findMoveProject(file)
    }

    val allProjects: List<MoveProject>
        get() = this.projects.state

    private fun doRefresh(project: Project): CompletableFuture<List<MoveProject>> {
        val result = CompletableFuture<List<MoveProject>>()
        val syncTask = MoveProjectsSyncTask(project, result)
        project.taskQueue.run(syncTask)
        return result.thenApply { updatedProjects ->
            runOnlyInNonLightProject(project) {
                setupProjectRoots(project, updatedProjects)
            }
            updatedProjects
        }
    }

    private fun findMoveProject(file: VirtualFile): MoveProject? {
        val cached = this.projectsIndex.get(file)
        if (cached is IndexEntry.Present) {
            return cached.value
        }
        if (isUnitTestMode && file.fileSystem is TempFileSystem) {
            return MoveProject.forTests(project)
        }

        val filePath = file.toNioPathOrNull() ?: return null
        var moveProject: MoveProject? = null
        for (candidate in this.projects.state) {
            if (moveProject != null) break
            for (movePackage in candidate.movePackages()) {
                val packageRoot = movePackage.contentRoot.toNioPathOrNull() ?: continue
                if (filePath.startsWith(packageRoot)) {
                    moveProject = candidate
                    break
                }
            }
        }
        this.projectsIndex.put(file, IndexEntry.Present(moveProject))
        return moveProject
    }


    /**
     * The heart of the plugin Project model. Care must be taken to ensure
     * this is thread-safe, and that refreshes are scheduled after
     * set of projects changes.
     */
    private val projects = AsyncValue<List<MoveProject>>(emptyList())

    private val projectsIndex: MoveProjectsIndex = MoveProjectsIndex(project, {})

    /**
     * All modifications to project model except for low-level `loadState` should
     * go through this method: it makes sure that when we update various IDEA listeners.
     */
    private fun modifyProjects(
        updater: (List<MoveProject>) -> CompletableFuture<List<MoveProject>>
    ): CompletableFuture<List<MoveProject>> {
        val refreshStatusPublisher =
            project.messageBus.syncPublisher(MOVE_PROJECTS_REFRESH_TOPIC)

        val wrappedUpdater = { projects: List<MoveProject> ->
            refreshStatusPublisher.onRefreshStarted()
            updater(projects)
        }

        return projects.updateAsync(wrappedUpdater)
            .thenApply { projects ->
                buildWatcher.updateProjects(projects)
                resetIDEState(projects)
                projects
            }
    }

    private fun resetIDEState(projects: Collection<MoveProject>) {
        invokeAndWaitIfNeeded {
            runWriteAction {
                projectsIndex.resetIndex()

                MoveElementsIndex.requestRebuild()
                PsiManager.getInstance(project).dropPsiCaches()

                // In unit tests roots change is done by the test framework in most cases
                runOnlyInNonLightProject(project) {
                    ProjectRootManagerEx.getInstanceEx(project)
                        .makeRootsChange(EmptyRunnable.getInstance(), false, true)
                }
                project.messageBus.syncPublisher(MOVE_PROJECTS_TOPIC)
                    .moveProjectsUpdated(this, projects)
            }
        }
    }

    override fun dispose() {}

    companion object {
        private val LOG = logger<MoveProjectsService>()

        val MOVE_PROJECTS_TOPIC: Topic<MoveProjectsListener> = Topic(
            "move projects changes",
            MoveProjectsListener::class.java
        )

        val MOVE_PROJECTS_REFRESH_TOPIC: Topic<MoveProjectsRefreshListener> = Topic(
            "Move refresh",
            MoveProjectsRefreshListener::class.java
        )
    }

    fun interface MoveProjectsListener {
        fun moveProjectsUpdated(service: MoveProjectsService, projects: Collection<MoveProject>)
    }

    interface MoveProjectsRefreshListener {
        fun onRefreshStarted()
        fun onRefreshFinished(status: MoveRefreshStatus)
    }

    enum class MoveRefreshStatus {
        SUCCESS,
        FAILURE,
        CANCEL
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
