package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.ide.MvIcons
import javax.swing.Icon

class MvProjectOpenProcessor : ProjectOpenProcessor() {
    override fun getName(): String = "Move"
    override fun getIcon(): Icon = MvIcons.MOVE

    override fun canOpenProject(file: VirtualFile): Boolean =
        FileUtil.namesEqual(file.name, MvConstants.MANIFEST_FILE)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean,
    ): Project? {
        val platformOpenProcessor = PlatformProjectOpenProcessor.getInstance()
        return platformOpenProcessor.doOpenProject(
            virtualFile,
            projectToClose,
            forceOpenInNewFrame
        )?.also {
            StartupManager.getInstance(it).runWhenProjectIsInitialized {
                it.moveProjects.refreshAllProjects()
            }
        }
    }
}
