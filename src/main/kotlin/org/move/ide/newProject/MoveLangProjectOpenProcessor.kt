package org.move.ide.newProject

import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.cli.Consts
import org.move.cli.moveProjectsService
import org.move.cli.settings.Blockchain
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.openapiext.rootDir
import javax.swing.Icon

class InferBlockchainTypeOnStartupActivity: ProjectActivity {
    override suspend fun execute(project: Project) {
        RunOnceUtil.runOnceForProject(project, ID) {
            val moveToml = project.rootDir?.findChild("Move.toml") ?: return@runOnceForProject
            val moveTomlText = moveToml.readText()
            val blockchain =
                when {
                    moveTomlText.contains("https://github.com/MystenLabs/sui.git") -> Blockchain.SUI
                    else -> Blockchain.APTOS
                }

            if (project.moveSettings.blockchain != blockchain) {
                project.moveSettings.modify {
                    it.blockchain = blockchain
                }
            }
        }
    }

    companion object {
        const val ID = "org.move.InferBlockchainTypeOnStartupActivity"
    }
}

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

