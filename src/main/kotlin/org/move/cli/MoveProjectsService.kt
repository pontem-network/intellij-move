package org.move.cli

import com.intellij.execution.RunManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.util.messages.Topic
import org.jetbrains.rpc.LOG
import org.move.lang.findMoveTomlPath
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.findVirtualFile
import org.move.settings.MoveProjectSettingsService
import org.move.settings.MoveSettingsChangedEvent
import org.move.settings.MoveSettingsListener
import org.move.stdext.AsyncValue
import org.move.stdext.MyLightDirectoryIndex
import org.move.stdext.deepIterateChildrenRecursivery
import java.util.concurrent.CompletableFuture

val Project.moveProjectsService: MoveProjectsService get() = this.getService(MoveProjectsService::class.java)

interface MoveProjectsService {
//    var initialized: Boolean

    fun refreshAllProjects()

    fun findMoveProjectForPsiFile(psiFile: PsiFile): MoveProject?
//    fun findNamedAddressesForFile(scope: GlobalScope, file: VirtualFile): AddressesMap
//    fun findNamedAddressValueForFile(scope: GlobalScope, file: VirtualFile, name: String): String

    companion object {
        val MOVE_PROJECTS_TOPIC: Topic<MoveProjectsListener> = Topic(
            "move projects changes",
            MoveProjectsListener::class.java
        )
    }

    fun interface MoveProjectsListener {
        fun moveProjectsUpdated(service: MoveProjectsService, projects: Collection<MoveProject>)
    }
}

class MoveProjectsServiceImpl(val project: Project): MoveProjectsService {
    init {
        with(project.messageBus.connect()) {
            if (!isUnitTestMode) {
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

    /**
     * The heart of the plugin Project model. Care must be taken to ensure
     * this is thread-safe, and that refreshes are scheduled after
     * set of projects changes.
     */
    val projects = AsyncValue<List<MoveProject>>(emptyList())

    /**
     * All modifications to project model except for low-level `loadState` should
     * go through this method: it makes sure that when we update various IDEA listeners.
     */
    private fun modifyProjects(
        f: (List<MoveProject>) -> CompletableFuture<List<MoveProject>>
    ): CompletableFuture<List<MoveProject>> =
        projects.updateAsync(f)
            .thenApply { projects ->
                invokeAndWaitIfNeeded {
                    runWriteAction {
                        directoryIndex.resetIndex()
                        // In unit tests roots change is done by the test framework in most cases
                        runWithNonLightProject(project) {
                            ProjectRootManagerEx.getInstanceEx(project)
                                .makeRootsChange(EmptyRunnable.getInstance(), false, true)
                        }
                        project.messageBus.syncPublisher(MoveProjectsService.MOVE_PROJECTS_TOPIC)
                            .moveProjectsUpdated(this, projects)
//                        initialized = true
                    }
                }

                projects
            }

    override fun findMoveProjectForPsiFile(psiFile: PsiFile): MoveProject? {
        val file = psiFile.originalFile.virtualFile

        val cachedProject = this.directoryIndex.getInfoForFile(file)
        if (cachedProject != null) return cachedProject

//        if (isUnitTestMode) {
//            return testEmptyMoveProject(project)
//        }

        LOG.warn("MoveProject is not found in cache")
        val moveProject =
            file.toNioPathOrNull()
                ?.let { findMoveTomlPath(it) }
                ?.findVirtualFile()
                ?.let { initializeMoveProject(project, it) }
        if (moveProject == null && isUnitTestMode) {
            // this is for light tests, heavy test should always have valid moveProject
            return testEmptyMoveProject(project)
        }
//        val moveProject = initializeMoveProject(project, moveTomlFile)
//        val moveProject = MoveProject.fromMoveTomlPath(project, moveTomlFile) ?: return null
        this.directoryIndex.putInfo(file, moveProject)
        return moveProject
    }

//    override var initialized: Boolean = false

    override fun refreshAllProjects() {
        LOG.info("Project state refresh started")
        modifyProjects { doRefresh(project) }
    }

    private val directoryIndex: MyLightDirectoryIndex<MoveProject?> =
        MyLightDirectoryIndex(project, null) { index ->
            processAllMoveFilesOnce(this.projects.currentState) { file, moveProject ->
                index.putInfo(file, moveProject)
            }
        }
}

private fun doRefresh(project: Project): CompletableFuture<List<MoveProject>> {
    val result = CompletableFuture<List<MoveProject>>()
    val syncTask = MoveSyncTask(project, result)
    project.taskQueue.run(syncTask)
    return result.thenApply { updatedProjects ->
        runWithNonLightProject(project) {
            setupProjectRoots(project, updatedProjects)
        }
        updatedProjects
    }
}

private fun setupProjectRoots(project: Project, moveProjects: List<MoveProject>) {
    invokeAndWaitIfNeeded {
        // Initialize services that we use (probably indirectly) in write action below.
        // Otherwise, they can be initialized in write action that may lead to deadlock
        RunManager.getInstance(project)
        ProjectFileIndex.getInstance(project)

        runWriteAction {
            if (project.isDisposed) return@runWriteAction
            ProjectRootManagerEx.getInstanceEx(project).mergeRootsChangesDuring {
                for (moveProject in moveProjects) {
                    moveProject.root.setupContentRoots(project) { contentRoot ->
                        addExcludeFolder(
                            FileUtil.join(
                                contentRoot.url,
                                MoveConstants.ProjectLayout.build
                            )
                        )
                    }
                }
            }
        }
    }
}

private inline fun runWithNonLightProject(project: Project, action: () -> Unit) {
    if ((project as? ProjectEx)?.isLight != true) {
        action()
    } else {
        check(isUnitTestMode)
    }
}

//val Path.depth: Int get() = this.split("/").count()
val VirtualFile.fsDepth: Int get() = this.path.split("/").count()
//val String.pathDepth: Int get() = this.split("/").count()

fun processAllMoveFilesOnce(
    moveProjects: List<MoveProject>,
    processFile: (VirtualFile, MoveProject) -> Unit
) {
    val visited = mutableSetOf<String>()
    // find Move.toml files in all project roots, remembering depth of those
    // then search all .move children of those files, with biggest depth first
    moveProjects
        .sortedByDescending { it.root.fsDepth }
        .map { project ->
            deepIterateChildrenRecursivery(project.root, { it.extension == "move" }) {
                if (it.path in visited) return@deepIterateChildrenRecursivery true
                visited.add(it.path)
                processFile(it, project)
                true
            }
        }
}

private fun VirtualFile.setupContentRoots(project: Project, setup: ContentEntry.(VirtualFile) -> Unit) {
    val packageModule = ModuleUtilCore.findModuleForFile(this, project) ?: return
    setupContentRoots(packageModule, setup)
}

private fun VirtualFile.setupContentRoots(packageModule: Module, setup: ContentEntry.(VirtualFile) -> Unit) {
    ModuleRootModificationUtil.updateModel(packageModule) { rootModel ->
        rootModel.contentEntries.singleOrNull()?.setup(this)
    }
}