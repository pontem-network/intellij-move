package org.move.cli

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.cli.runConfigurations.addDefaultBuildRunConfiguration
import org.move.cli.settings.MoveProjectSettingsService
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.ide.newProject.openFile
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.aptosBuildRunConfigurations
import org.move.openapiext.aptosRunConfigurations
import org.move.openapiext.contentRoots
import javax.swing.Icon

class AptosProjectOpenProcessor: ProjectOpenProcessor() {
    override val name: String get() = "Move"
    override val icon: Icon get() = MoveIcons.MOVE_LOGO

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

                // opens Move.toml file
                val packageRoot = it.contentRoots.firstOrNull()
                if (packageRoot != null) {
                    val manifest = packageRoot.findChild(Consts.MANIFEST_FILE)
                    if (manifest != null) {
                        it.openFile(manifest)
                    }
                    updateAllNotifications(it)
                }

                val defaultProjectSettings = ProjectManager.getInstance().defaultMoveSettings
                it.moveSettings.modify {
                    it.aptosPath = defaultProjectSettings?.state?.aptosPath
                }

                it.moveProjects.refreshAllProjects()
//
//                val aptosCliPath = AptosCliExecutor.suggestPath()
//                if (aptosCliPath != null && it.aptosPath?.toString().isNullOrBlank()) {
//                    it.moveSettings.modify { state ->
//                        state.aptosPath = aptosCliPath
//                    }
//                }
            }
        }
    }
}

fun defaultProjectSettings(): MoveProjectSettingsService? = ProjectManager.getInstance().defaultMoveSettings

val ProjectManager.defaultMoveSettings: MoveProjectSettingsService?
    get() = this.defaultProject.getService(MoveProjectSettingsService::class.java)
