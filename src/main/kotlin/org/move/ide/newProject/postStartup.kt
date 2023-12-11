package org.move.ide.newProject

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.move.cli.moveProjectsService
import org.move.openapiext.common.isUnitTestMode
import org.move.openapiext.infoInProduction

//class CreateDefaultCompileRunConfiguration: ProjectActivity {
//    override suspend fun execute(project: Project) {
//        LOG.infoInProduction("activity started")
//        RunOnceUtil.runOnceForProject(project, DEFAULT_COMPILE_CONFIGURATION_CREATED) {
//            LOG.infoInProduction("activity executed")
//            // create default build configuration if it doesn't exist
//            if (project.runManager.allConfigurationsList.isEmpty()) {
//                project.addCompileProjectRunConfiguration(true)
//            }
//        }
//    }
//
//    companion object {
//        const val DEFAULT_COMPILE_CONFIGURATION_CREATED = "org.move.CreateDefaultBuildRunConfigurationActivity"
//
//        private val LOG = logger<CreateDefaultCompileRunConfiguration>()
//    }
//}

//class OpenMoveTomlOnProjectCreationFile: ProjectActivity {
//    override suspend fun execute(project: Project) {
//        LOG.infoInProduction("activity started")
//        RunOnceUtil.runOnceForProject(project, TOML_FILE_OPENED) {
//            LOG.infoInProduction("activity executed")
//            // opens Move.toml file
//            val manifestFile = project.rootDir?.findChild(Consts.MANIFEST_FILE)
//            if (manifestFile != null) {
//                project.openFile(manifestFile)
//            }
//            updateAllNotifications(project)
//        }
//    }
//
//    companion object {
//        const val TOML_FILE_OPENED = "org.move.OpenMoveTomlOnProjectCreationFileActivity"
//
//        private val LOG = logger<OpenMoveTomlOnProjectCreationFile>()
//    }
//}

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

//fun createDefaultBuildConfiguration(project: Project) {
//    // create default build configuration if it doesn't exist
//    if (project.aptosBuildRunConfigurations().isEmpty()) {
//        val isEmpty = project.aptosCommandConfigurations().isEmpty()
//        project.addDefaultBuildRunConfiguration(isSelected = isEmpty)
//    }
//
//    // opens Move.toml file
//    val packageRoot = project.contentRoots.firstOrNull()
//    if (packageRoot != null) {
//        val manifest = packageRoot.findChild(Consts.MANIFEST_FILE)
//        if (manifest != null) {
//            project.openFile(manifest)
//        }
//        updateAllNotifications(project)
//    }
//
//    val defaultProjectSettings = ProjectManager.getInstance().defaultMoveSettings
//    project.moveSettings.modify {
//        it.aptosPath = defaultProjectSettings?.state?.aptosPath
//    }
//
//    project.moveProjectsService.scheduleProjectsRefresh()
//}

//val ProjectManager.defaultMoveSettings: MoveProjectSettingsService?
//    get() = this.defaultProject.getService(MoveProjectSettingsService::class.java)
