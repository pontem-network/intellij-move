package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import org.jetbrains.rpc.LOG
import org.move.lang.findMoveTomlPath
import org.move.lang.toNioPathOrNull
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.contentRoots
import org.move.settings.MoveProjectSettingsService
import org.move.settings.MoveSettingsChangedEvent
import org.move.settings.MoveSettingsListener
import org.move.stdext.MyLightDirectoryIndex
import org.move.stdext.deepIterateChildrenRecursivery

val Project.moveProjects: MoveProjectsService get() = this.getService(MoveProjectsService::class.java)

interface MoveProjectsService {
    fun findMoveProjectForPsiFile(psiFile: PsiFile): MoveProject?
//    fun findNamedAddressesForFile(scope: GlobalScope, file: VirtualFile): AddressesMap
//    fun findNamedAddressValueForFile(scope: GlobalScope, file: VirtualFile, name: String): String
}

class MoveProjectsServiceImpl(val project: Project) : MoveProjectsService {
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

    override fun findMoveProjectForPsiFile(psiFile: PsiFile): MoveProject? {
        val file = psiFile.originalFile.virtualFile

        val cachedPath = this.directoryIndex.getInfoForFile(file)
        if (cachedPath != null) return cachedPath

        val moveTomlPath =
            file.toNioPathOrNull()?.let { findMoveTomlPath(it) } ?: return null
        val moveProject = MoveProject.fromMoveTomlPath(project, moveTomlPath) ?: return null
        this.directoryIndex.putInfo(file, moveProject)
        return moveProject
    }

    private fun refreshAllProjects() {
        LOG.warn("Project state is refreshed")
        directoryIndex.resetIndex()
    }

    private val directoryIndex: MyLightDirectoryIndex<MoveProject?> =
        MyLightDirectoryIndex(project, null) { index ->
            processAllMoveFilesOnce(project) { file, moveProject ->
                index.putInfo(file, moveProject)
            }
        }
}

fun findMoveFilesDeepestFirst(project: Project): Sequence<VirtualFile> {
    val contentRoots = project.contentRoots
    val moveFiles = mutableSetOf<Pair<Int, VirtualFile>>()
    for (contentRoot in contentRoots) {
        deepIterateChildrenRecursivery(
            contentRoot, { it.name == "Move.toml" })
        {
            val depth = it.path.split("/").count()
            moveFiles.add(Pair(depth, it))
            true
        }
    }
    return moveFiles.asSequence().sortedByDescending { it.first }.map { it.second }
}

fun processAllMoveFilesOnce(project: Project, processFile: (VirtualFile, MoveProject) -> Unit) {
    val visited = mutableSetOf<String>()
    // find Move.toml files in all project roots, remembering depth of those
    // then search all .move children of those files, with biggest depth first
    val moveFiles = findMoveFilesDeepestFirst(project).toList()
    for (moveTomlFile in moveFiles) {
        val root = moveTomlFile.parent
        val moveTomlPath = moveTomlFile.toNioPathOrNull() ?: continue
        val moveProject = MoveProject.fromMoveTomlPath(project, moveTomlPath) ?: continue
        deepIterateChildrenRecursivery(root, { it.extension == "move" }) {
            if (it.path in visited) return@deepIterateChildrenRecursivery true
            visited.add(it.path)
            processFile(it, moveProject)
            true
        }
    }
}
