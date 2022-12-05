package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.cli.runconfig.addDefaultBuildRunConfiguration
import org.move.cli.settings.aptosPath
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.ide.newProject.openFile
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.aptosBuildRunConfigurations
import org.move.openapiext.aptosRunConfigurations
import org.move.openapiext.contentRoots
import javax.swing.Icon

class MoveProjectOpenProcessor : ProjectOpenProcessor() {
    override fun getName(): String = "Move"
    override fun getIcon() = MoveIcons.MOVE

    override fun canOpenProject(file: VirtualFile): Boolean =
        FileUtil.namesEqual(file.name, Consts.MANIFEST_FILE)
                || (file.isDirectory && file.findChild(Consts.MANIFEST_FILE) != null)

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
            StartupManager.getInstance(it).runAfterOpened {
                // create default build configuration if it doesn't exist
                if (it.aptosBuildRunConfigurations().isEmpty()) {
                    val isEmpty = it.aptosRunConfigurations().isEmpty()
                    it.addDefaultBuildRunConfiguration(isSelected = isEmpty)
                }

                val packageRoot = it.contentRoots.firstOrNull()
                if (packageRoot != null) {
                    val manifest = packageRoot.findChild(Consts.MANIFEST_FILE)
                    if (manifest != null) {
                        it.openFile(manifest)
                    }
                    updateAllNotifications(it)
                }

                val aptosPath = Aptos.suggestPath()
                if (aptosPath != null && it.aptosPath?.toString().isNullOrBlank()) {
                    it.moveSettings.modify {
                        it.aptosPath = aptosPath
                    }
                }
                it.moveProjects.refreshAllProjects()
            }
        }
    }
}
