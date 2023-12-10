package org.move.cli.openProject

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.move.cli.Consts
import org.move.cli.moveProjectsService
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
        )
    }
}

fun defaultProjectSettings(): MoveProjectSettingsService? = ProjectManager.getInstance().defaultMoveSettings

val ProjectManager.defaultMoveSettings: MoveProjectSettingsService?
    get() = this.defaultProject.getService(MoveProjectSettingsService::class.java)
