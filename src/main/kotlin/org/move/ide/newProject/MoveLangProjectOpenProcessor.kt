package org.move.ide.newProject

import com.intellij.openapi.project.DumbAwareRunnable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.cli.MvConstants
import org.move.ide.MoveIcons
import javax.swing.Icon

/// called only when IDE opens a project from existing sources
class MoveLangProjectOpenProcessor: ProjectOpenProcessor() {
    override val name: String get() = "Move"
    override val icon: Icon get() = MoveIcons.MOVE_LOGO

    override fun canOpenProject(file: VirtualFile): Boolean {
        val canBeOpened = (FileUtil.namesEqual(file.name, MvConstants.MANIFEST_FILE)
                || (file.isDirectory && file.findChild(MvConstants.MANIFEST_FILE) != null))
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
                @Suppress("DEPRECATION")
                StartupManager.getInstance(project)
                    .runWhenProjectIsInitialized(object: DumbAwareRunnable {
                        override fun run() {
                            ProjectInitializationSteps.createDefaultCompileConfigurationIfNotExists(project)
                            // NOTE:
                            // this cannot be moved to a ProjectActivity, as Move.toml files
                            // are not created by the time those activities are executed
                            ProjectInitializationSteps.openMoveTomlInEditor(project)
                        }
                    })
            }
    }
}

