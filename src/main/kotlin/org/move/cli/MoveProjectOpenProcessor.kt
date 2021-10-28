package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.ide.MoveIcons
import javax.swing.Icon

class MoveProjectOpenProcessor : ProjectOpenProcessor() {
    override fun getName(): String = "Move"
    override fun getIcon(): Icon = MoveIcons.MOVE

    override fun canOpenProject(file: VirtualFile): Boolean =
        FileUtil.namesEqual(file.name, Constants.MOVE_MANIFEST_FILE)

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
        )
    }
}
