package org.move.ide.newProject

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.cli.Consts
import org.move.ide.MoveIcons
import javax.swing.Icon

/// called only when IDE opens a project from existing sources
class MoveLangProjectOpenProcessor: ProjectOpenProcessor() {
    override val name: String get() = "Move"
    override val icon: Icon get() = MoveIcons.MOVE_LOGO

    override fun canOpenProject(file: VirtualFile): Boolean {
        val canBeOpened = (FileUtil.namesEqual(file.name, Consts.MANIFEST_FILE)
                || (file.isDirectory && file.findChild(Consts.MANIFEST_FILE) != null))
        return canBeOpened
    }

    override fun doOpenProject(
        virtualFile: VirtualFile,
        projectToClose: Project?,
        forceOpenInNewFrame: Boolean,
    ): Project? {
        val platformOpenProcessor = PlatformProjectOpenProcessor.getInstance()
        val basedir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent
        return platformOpenProcessor.doOpenProject(basedir, projectToClose, forceOpenInNewFrame)
            ?.also { project ->
                StartupManager.getInstance(project)
                    .runWhenProjectIsInitialized {
                        ProjectInitializationSteps.openMoveTomlInEditor(project)
                        ProjectInitializationSteps.createDefaultCompileConfigurationIfNotExists(project)
                    }
            }
    }
}

