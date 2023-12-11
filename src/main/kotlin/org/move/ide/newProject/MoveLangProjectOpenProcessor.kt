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
                        ProjectInitialization.openMoveTomlInEditor(project)
                        ProjectInitialization.createDefaultCompileConfigurationIfNotExists(project)
                    }
            }
//            ?.also {
//            StartupManager.getInstance(it).runAfterOpened {
//                // create default build configuration if it doesn't exist
//                if (it.aptosBuildRunConfigurations().isEmpty()) {
//                    val isEmpty = it.aptosRunConfigurations().isEmpty()
//                    it.addDefaultBuildRunConfiguration(isSelected = isEmpty)
//                }
//
//                // opens Move.toml file
//                val packageRoot = it.contentRoots.firstOrNull()
//                if (packageRoot != null) {
//                    val manifest = packageRoot.findChild(Consts.MANIFEST_FILE)
//                    if (manifest != null) {
//                        it.openFile(manifest)
//                    }
//                    updateAllNotifications(it)
//                }
//
//                val defaultProjectSettings = ProjectManager.getInstance().defaultMoveSettings
//                it.moveSettings.modify {
//                    it.aptosPath = defaultProjectSettings?.state?.aptosPath
//                }
//
//                it.moveProjectsService.scheduleProjectsRefresh()
////
////                val aptosCliPath = AptosCliExecutor.suggestPath()
////                if (aptosCliPath != null && it.aptosPath?.toString().isNullOrBlank()) {
////                    it.moveSettings.modify { state ->
////                        state.aptosPath = aptosCliPath
////                    }
////                }
//            }
//    }
    }
//
//    companion object {
//        private val LOG = logger<MoveLangProjectOpenProcessor>()
//    }
}

//fun defaultProjectSettings(): MoveProjectSettingsService? = ProjectManager.getInstance().defaultMoveSettings

