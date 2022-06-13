package org.move.cli

import com.intellij.execution.RunManager
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
import com.intellij.util.messages.Topic
import org.move.cli.settings.MoveProjectSettingsService
import org.move.cli.settings.MoveSettingsChangedEvent
import org.move.cli.settings.MvSettingsListener
import org.move.lang.MoveFile
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.toVirtualFile
import org.move.stdext.AsyncValue
import org.move.stdext.MoveProjectsIndex
import org.move.stdext.IndexEntry
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

val Project.moveProjects get() = service<MoveProjectsService>()

enum class DevMode {
    MAIN, DEV;
}

class MoveProjectsService(val project: Project) {
    init {
        with(project.messageBus.connect()) {
            if (!isUnitTestMode) {
                subscribe(VirtualFileManager.VFS_CHANGES, MoveTomlWatcher {
                    refresh()
                })
            }
            subscribe(MoveProjectSettingsService.MOVE_SETTINGS_TOPIC, object : MvSettingsListener {
                override fun moveSettingsChanged(e: MoveSettingsChangedEvent) {
                    refresh()
                }
            })
        }
    }

    fun refresh() {
        LOG.info("Project state refresh started")
        modifyProjects {
            doRefresh(project)
        }
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
        f: (List<MoveProject>) -> CompletableFuture<List<MoveProject>>
    ): CompletableFuture<List<MoveProject>> =
        projects
            .updateAsync(f)
            .thenApply { projects ->
                resetIDEState(projects)
                projects
            }

    private fun resetIDEState(projects: Collection<MoveProject>) {
        invokeAndWaitIfNeeded {
            runWriteAction {
                projectsIndex.resetIndex()
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

    companion object {
        private val LOG = logger<MoveProjectsService>()

        val MOVE_PROJECTS_TOPIC: Topic<MoveProjectsListener> = Topic(
            "move projects changes",
            MoveProjectsListener::class.java
        )

        fun interface MoveProjectsListener {
            fun moveProjectsUpdated(service: MoveProjectsService, projects: Collection<MoveProject>)
        }

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
            moveProject.processMoveFiles(DevMode.DEV) {
                val filePath = it.virtualFile.path
                if (filePath in visited) return@processMoveFiles true
                visited.add(filePath)

                processFile(it, moveProject)
                true
            }
        }
}
