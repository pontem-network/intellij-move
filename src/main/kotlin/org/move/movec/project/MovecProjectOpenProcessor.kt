package org.move.movec.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.movec.MovecConstants

class MovecProjectOpenProcessor : ProjectOpenProcessor() {
    override fun getName(): String = "Movec"

    override fun canOpenProject(file: VirtualFile): Boolean =
        FileUtil.namesEqual(file.name, MovecConstants.MANIFEST_FILE)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceNewFrame: Boolean,
    ): Project? {
        val basedir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent

        return PlatformProjectOpenProcessor.getInstance()
            .doOpenProject(basedir, projectToClose, forceNewFrame)?.also {
                StartupManager.getInstance(it).runWhenProjectIsInitialized { setupMoveProject(it) }
            }
    }

}