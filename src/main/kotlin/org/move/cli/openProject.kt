package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.ide.MoveIcons
import javax.swing.Icon

class DoveProjectOpenProcessor : ProjectOpenProcessor() {
    override fun getName(): String = "Move"
    override fun getIcon(): Icon = MoveIcons.MOVE

    override fun canOpenProject(file: VirtualFile): Boolean =
        FileUtil.namesEqual(file.name, DoveConstants.MANIFEST_FILE)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean,
    ): Project? {
//        val basedir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent

        val platformOpenProcessor = PlatformProjectOpenProcessor.getInstance()
        val project = platformOpenProcessor.doOpenProject(virtualFile,
                                                          projectToClose,
                                                          forceOpenInNewFrame) ?: return null

//        StartupManager.getInstance(project).runWhenProjectIsInitialized { setupDoveProject(project) }
        return project
//        return PlatformProjectOpenProcessor.getInstance()
//            .doOpenProject(basedir, projectToClose, forceOpenInNewFrame)?.also {
//                StartupManager.getInstance(it).runWhenProjectIsInitialized { setupDoveProject(it) }
//            }
    }
}

//fun setupDoveProject(project: Project) {
//    println("running setupDoveProject")
//    // instantiate service to bind subscribers
//    ServiceManager.getService(project, DoveProjectService::class.java)
////    val moveSettings = ServiceManager.getService(MoveProjectSettingsService::class.java)
//////    val doveExecutable = moveSettings.doveExecutable
//}