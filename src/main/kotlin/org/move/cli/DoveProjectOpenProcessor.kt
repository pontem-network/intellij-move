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
        FileUtil.namesEqual(file.name, Constants.DOVE_MANIFEST_FILE)

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean,
    ): Project? {
        val platformOpenProcessor = PlatformProjectOpenProcessor.getInstance()
        val project = platformOpenProcessor.doOpenProject(virtualFile,
                                                          projectToClose,
                                                          forceOpenInNewFrame) ?: return null
        return project
    }
}
