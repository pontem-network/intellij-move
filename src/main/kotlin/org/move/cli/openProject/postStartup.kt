package org.move.cli.openProject

import com.intellij.ide.util.RunOnceUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.ProjectActivity
import org.move.cli.Consts
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.addCompileProjectRunConfiguration
import org.move.cli.runConfigurations.addDefaultBuildRunConfiguration
import org.move.cli.settings.moveSettings
import org.move.ide.newProject.openFile
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.*
import org.move.openapiext.common.isUnitTestMode

class CreateDefaultCompileRunConfiguration: ProjectActivity {
    override suspend fun execute(project: Project) {
        RunOnceUtil.runOnceForProject(project, DEFAULT_COMPILE_CONFIGURATION_CREATED) {
            LOG.infoInProduction("activity started")
            // create default build configuration if it doesn't exist
            if (project.runManager.allConfigurationsList.isEmpty()) {
                project.addCompileProjectRunConfiguration(true)
            }
        }
    }

    companion object {
        const val DEFAULT_COMPILE_CONFIGURATION_CREATED = "org.move.CreateDefaultBuildRunConfigurationActivity"

        private val LOG = logger<CreateDefaultCompileRunConfiguration>()
    }
}

class OpenMoveTomlOnProjectCreationFile: ProjectActivity {
    override suspend fun execute(project: Project) {
        RunOnceUtil.runOnceForProject(project, TOML_FILE_OPENED) {
            LOG.infoInProduction("activity started")
            // opens Move.toml file
            val manifestFile = project.rootDir?.findChild(Consts.MANIFEST_FILE)
            if (manifestFile != null) {
                project.openFile(manifestFile)
            }
            updateAllNotifications(project)
        }
    }

    companion object {
        const val TOML_FILE_OPENED = "org.move.OpenMoveTomlOnProjectCreationFileActivity"

        private val LOG = logger<OpenMoveTomlOnProjectCreationFile>()
    }
}

class AlwaysRefreshProjectsAfterOpen: ProjectActivity {
    override suspend fun execute(project: Project) {
        LOG.infoInProduction("activity started")
        if (!isUnitTestMode) {
            project.moveProjectsService.scheduleProjectsRefresh("IDE project opened")
        } else {
            LOG.warn("Skip REFRESH [IDE project opened] in unit tests")
        }
    }

    companion object {
        private val LOG = logger<AlwaysRefreshProjectsAfterOpen>()
    }
}

fun createDefaultBuildConfiguration(project: Project) {
    // create default build configuration if it doesn't exist
    if (project.aptosBuildRunConfigurations().isEmpty()) {
        val isEmpty = project.aptosRunConfigurations().isEmpty()
        project.addDefaultBuildRunConfiguration(isSelected = isEmpty)
    }

    // opens Move.toml file
    val packageRoot = project.contentRoots.firstOrNull()
    if (packageRoot != null) {
        val manifest = packageRoot.findChild(Consts.MANIFEST_FILE)
        if (manifest != null) {
            project.openFile(manifest)
        }
        updateAllNotifications(project)
    }

    val defaultProjectSettings = ProjectManager.getInstance().defaultMoveSettings
    project.moveSettings.modify {
        it.aptosPath = defaultProjectSettings?.state?.aptosPath
    }

    project.moveProjectsService.scheduleProjectsRefresh()
}
