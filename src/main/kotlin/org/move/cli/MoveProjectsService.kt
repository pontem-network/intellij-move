package org.move.cli

import com.intellij.execution.RunManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.messages.Topic
import org.jetbrains.rpc.LOG
import org.move.cli.settings.MoveProjectSettingsService
import org.move.cli.settings.MoveSettingsChangedEvent
import org.move.cli.settings.MvSettingsListener
import org.move.lang.MoveFile
import org.move.lang.findMoveTomlPath
import org.move.lang.isMoveFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.toVirtualFile
import org.move.stdext.AsyncValue
import org.move.stdext.DirectoryIndexEntry
import org.move.stdext.MyLightDirectoryIndex
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

val Project.projectsService get() = service<MoveProjectsService>()

class MoveProjectsService(val project: Project) {
    init {
        with(project.messageBus.connect()) {
            if (!isUnitTestMode) {
                subscribe(VirtualFileManager.VFS_CHANGES, MoveTomlWatcher {
                    refreshAllProjects()
                })
            }
            subscribe(MoveProjectSettingsService.MOVE_SETTINGS_TOPIC, object : MvSettingsListener {
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

    fun findProjectForPsiElement(psiElement: PsiElement): MoveProject? {
        val file = when (psiElement) {
            is PsiDirectory -> psiElement.virtualFile
            is PsiFile -> psiElement.originalFile.virtualFile
            else -> psiElement.containingFile?.originalFile?.virtualFile
        }
        return findMoveProject(file)
    }

    fun findProjectForPath(path: Path): MoveProject? {
        val file = path.toVirtualFile() ?: return null
        return findMoveProject(file)
    }

    val allProjects: List<MoveProject>
        get() = this.projects.state

    private fun findMoveProject(file: VirtualFile?): MoveProject? {
        // in-memory file
        if (file == null) return null

        val cachedProjectEntry = this.directoryIndex.getInfoForFile(file)
        if (cachedProjectEntry is DirectoryIndexEntry.Present) {
            return cachedProjectEntry.project
        }
        LOG.warn("MoveProject is not found in cache")

        var moveProject = fun(): MoveProject? {
            val filePath = file.toNioPathOrNull() ?: return null
            val moveTomlPath = findMoveTomlPath(filePath) ?: return null

            if (file.isMoveFile) {
                val expectedRoot = moveTomlPath.parent
                val dirs = MvProjectLayout.dirs(expectedRoot)
                if (!dirs.any { filePath.startsWith(it) }) {
                    return null
                }
            }
            return moveTomlPath.toVirtualFile()?.let { initializeMoveProject(project, it) }
        }.invoke()
        if (moveProject == null && isUnitTestMode) {
            // this is for light tests, heavy test should always have valid moveProject
            moveProject = testEmptyMoveProject(project)
        }
        this.directoryIndex.putInfo(file, DirectoryIndexEntry.Present(moveProject))
        return moveProject
    }


    /**
     * The heart of the plugin Project model. Care must be taken to ensure
     * this is thread-safe, and that refreshes are scheduled after
     * set of projects changes.
     */
    private val projects = AsyncValue<List<MoveProject>>(emptyList())

    private val directoryIndex: MyLightDirectoryIndex<DirectoryIndexEntry> =
        MyLightDirectoryIndex(project, DirectoryIndexEntry.Missing) { index ->
            val projects = projects.state
            processAllMoveFilesOnce(projects) { moveFile, moveProject ->
                index.putInfo(moveFile.virtualFile, DirectoryIndexEntry.Present(moveProject))
            }
        }

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
                        runOnlyInNonLightProject(project) {
                            ProjectRootManagerEx.getInstanceEx(project)
                                .makeRootsChange(EmptyRunnable.getInstance(), false, true)
                        }
                        project.messageBus.syncPublisher(MOVE_PROJECTS_TOPIC)
                            .moveProjectsUpdated(this, projects)
                    }
                }

                projects
            }

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

//interface MoveProjectsService {
//    fun refreshAllProjects()
//
//    fun findProjectForPsiElement(psiElement: PsiElement): MoveProject?
//    fun findProjectForPath(path: Path): MoveProject?
//
//    val allProjects: Collection<MoveProject>
//
//    companion object {
//        val MOVE_PROJECTS_TOPIC: Topic<MoveProjectsListener> = Topic(
//            "move projects changes",
//            MoveProjectsListener::class.java
//        )
//    }
//
//    fun interface MoveProjectsListener {
//        fun moveProjectsUpdated(service: MoveProjectsService, projects: Collection<MoveProject>)
//    }
//}

//class MoveProjectsServiceImpl(val project: Project) : MoveProjectsService {
//    init {
//        with(project.messageBus.connect()) {
//            if (!isUnitTestMode) {
//                subscribe(VirtualFileManager.VFS_CHANGES, MoveTomlWatcher {
//                    refreshAllProjects()
//                })
//            }
//            subscribe(MoveProjectSettingsService.MOVE_SETTINGS_TOPIC, object : MvSettingsListener {
//                override fun moveSettingsChanged(e: MoveSettingsChangedEvent) {
//                    refreshAllProjects()
//                }
//            })
//        }
//    }
//
//    /**
//     * The heart of the plugin Project model. Care must be taken to ensure
//     * this is thread-safe, and that refreshes are scheduled after
//     * set of projects changes.
//     */
//    val projects = AsyncValue<List<MoveProject>>(emptyList())
//
//    /**
//     * All modifications to project model except for low-level `loadState` should
//     * go through this method: it makes sure that when we update various IDEA listeners.
//     */
//    private fun modifyProjects(
//        f: (List<MoveProject>) -> CompletableFuture<List<MoveProject>>
//    ): CompletableFuture<List<MoveProject>> =
//        projects.updateAsync(f)
//            .thenApply { projects ->
//                invokeAndWaitIfNeeded {
//                    runWriteAction {
//                        directoryIndex.resetIndex()
//                        // In unit tests roots change is done by the test framework in most cases
//                        runWithNonLightProject(project) {
//                            ProjectRootManagerEx.getInstanceEx(project)
//                                .makeRootsChange(EmptyRunnable.getInstance(), false, true)
//                        }
//                        project.messageBus.syncPublisher(MoveProjectsService.MOVE_PROJECTS_TOPIC)
//                            .moveProjectsUpdated(this, projects)
//                    }
//                }
//
//                projects
//            }
//
//    override fun refreshAllProjects() {
//        LOG.info("Project state refresh started")
//        modifyProjects { doRefresh(project) }
//    }
//
//    override fun findProjectForPsiElement(psiElement: PsiElement): MoveProject? {
//        val file = when (psiElement) {
//            is PsiDirectory -> psiElement.virtualFile
//            is PsiFile -> psiElement.originalFile.virtualFile
//            else -> psiElement.containingFile?.originalFile?.virtualFile
//        }
//        return findMoveProject(file)
//    }
//
//    override fun findProjectForPath(path: Path): MoveProject? {
//        val file = path.toVirtualFile() ?: return null
//        return findMoveProject(file)
//    }
//
//    override val allProjects: Collection<MoveProject>
//        get() = this.projects.currentState
//
//    private fun findMoveProject(file: VirtualFile?): MoveProject? {
//        // in-memory file
//        if (file == null) return null
//
//        val cachedProjectEntry = this.directoryIndex.getInfoForFile(file)
//        if (cachedProjectEntry is MoveProjectEntry.Present) {
//            return cachedProjectEntry.project
//        }
//        LOG.warn("MoveProject is not found in cache")
//
//        var moveProject = fun(): MoveProject? {
//            val filePath = file.toNioPathOrNull() ?: return null
//            val moveTomlPath = findMoveTomlPath(filePath) ?: return null
//
//            if (file.isMoveFile) {
//                val expectedRoot = moveTomlPath.parent
//                val dirs = MvProjectLayout.dirs(expectedRoot)
//                if (!dirs.any { filePath.startsWith(it) }) {
//                    return null
//                }
//            }
//            return moveTomlPath.toVirtualFile()?.let { initializeMoveProject(project, it) }
//        }.invoke()
//        if (moveProject == null && isUnitTestMode) {
//            // this is for light tests, heavy test should always have valid moveProject
//            moveProject = testEmptyMvProject(project)
//        }
//        this.directoryIndex.putInfo(file, MoveProjectEntry.Present(moveProject))
//        return moveProject
//    }
//
//    private val directoryIndex: MyLightDirectoryIndex<MoveProjectEntry> =
//        MyLightDirectoryIndex(project, MoveProjectEntry.Missing) { index ->
//            processAllMvFilesOnce(this.projects.currentState) { file, moveProject ->
//                index.putInfo(file, MoveProjectEntry.Present(moveProject))
//            }
//        }
//}

private fun doRefresh(project: Project): CompletableFuture<List<MoveProject>> {
    val result = CompletableFuture<List<MoveProject>>()
    val syncTask = MvSyncTask(project, result)
    project.taskQueue.run(syncTask)
    return result.thenApply { updatedProjects ->
        runOnlyInNonLightProject(project) {
            setupProjectRoots(project, updatedProjects)
        }
        updatedProjects
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
                    moveProject.setupContentRoots(project) { _, contentRoot ->
                        addExcludeFolder(FileUtil.join(contentRoot.url, "build"))
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

//val Path.depth: Int get() = this.split("/").count()
val VirtualFile.fsDepth: Int get() = this.path.split("/").count()
//val String.pathDepth: Int get() = this.split("/").count()

fun processAllMoveFilesOnce(
    moveProjects: List<MoveProject>,
    processFile: (MoveFile, MoveProject) -> Unit
) {
    val visited = mutableSetOf<String>()
    // find Move.toml files in all project roots, remembering depth of those
    // then search all .move children of those files, with biggest depth first
    moveProjects
        .sortedByDescending { it.contentRoot.fsDepth }
        .map { moveProject ->
            moveProject.walkMoveFiles {
                val filePath = it.virtualFile.path
                if (filePath in visited) return@walkMoveFiles true
                visited.add(filePath)

                processFile(it, moveProject)
                true
            }
        }
}

private fun MoveProject.setupContentRoots(
    project: Project,
    setup: ContentEntry.(ModifiableRootModel, VirtualFile) -> Unit
) {
    val packageModule = ModuleUtilCore.findModuleForFile(this.contentRoot, project) ?: return
    ModuleRootModificationUtil.updateModel(packageModule) { rootModel ->
        rootModel.contentEntries.singleOrNull()?.setup(rootModel, this.contentRoot)
    }
}
