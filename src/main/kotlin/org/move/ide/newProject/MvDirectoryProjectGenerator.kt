package org.move.ide.newProject

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import org.move.cli.moveProjects
import org.move.cli.runconfig.addDefaultBuildRunConfiguration
import org.move.cli.settings.AptosSettingsPanel
import org.move.cli.settings.MoveSettingsPanel
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.computeWithCancelableProgress
import org.move.stdext.unwrapOrThrow

data class NewProjectData(
    val data: MoveSettingsPanel.Data,
    val aptosInitEnabled: Boolean,
    val initData: AptosSettingsPanel.InitData
)

class MvDirectoryProjectGenerator : DirectoryProjectGeneratorBase<NewProjectData>(),
                                    CustomStepProjectGenerator<NewProjectData> {

    private var peer: MvProjectGeneratorPeer? = null

    override fun getName() = "Move"
    override fun getLogo() = MoveIcons.MOVE
    override fun createPeer(): ProjectGeneratorPeer<NewProjectData> =
        MvProjectGeneratorPeer().also { peer = it }

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        settings: NewProjectData,
        module: Module
    ) {
        val aptos = settings.data.aptos() ?: return
        val packageName = project.name

        val manifestFile = project.computeWithCancelableProgress("Generating Aptos project...") {
            aptos.move_init(
                project, module,
                rootDirectory = baseDir,
                packageName = packageName
            )
                .unwrapOrThrow() // TODO throw? really??

//            if (settings.aptosInitEnabled) {
//                aptos.init(
//                    project, module,
//                    privateKeyPath = settings.initData.privateKeyPath,
//                    faucetUrl = settings.initData.faucetUrl,
//                    restUrl = settings.initData.restUrl,
//                )
//                    .unwrapOrThrow()
//            }
        }


        project.moveSettings.modify {
            it.aptosPath = settings.data.aptosPath
        }
        project.addDefaultBuildRunConfiguration(isSelected = true)
        project.openFile(manifestFile)

        updateAllNotifications(project)
        project.moveProjects.refreshAllProjects()
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<NewProjectData>,
        callback: AbstractNewProjectStep.AbstractCallback<NewProjectData>
    ): AbstractActionWithPanel = MvProjectSettingsStep(projectGenerator)
}
