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
import org.move.cli.runConfigurations.addDefaultBuildRunConfiguration
import org.move.cli.settings.MoveSettingsPanel
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.computeWithCancelableProgress
import org.move.stdext.unwrapOrThrow

data class AptosProjectConfig(
    val aptosSettings: MoveSettingsPanel.Data,
//    val aptosInitEnabled: Boolean,
//    val initData: AptosSettingsPanel.InitData
)

class AptosProjectGenerator: DirectoryProjectGeneratorBase<AptosProjectConfig>(),
                             CustomStepProjectGenerator<AptosProjectConfig> {

//    private var peer: MvProjectGeneratorPeer? = null

    override fun getName() = "Aptos"
    override fun getLogo() = MoveIcons.APTOS
    override fun createPeer(): ProjectGeneratorPeer<AptosProjectConfig> = AptosProjectGeneratorPeer()
//        MvProjectGeneratorPeer().also { peer = it }

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        projectConfig: AptosProjectConfig,
        module: Module
    ) {
        val aptosCli = projectConfig.aptosSettings.aptosCli() ?: return
        val packageName = project.name

        val manifestFile = project.computeWithCancelableProgress("Generating Aptos project...") {
            val manifestFile = aptosCli.moveInit(
                project,
                module,
                rootDirectory = baseDir,
                packageName = packageName
            )
                .unwrapOrThrow() // TODO throw? really??

//            if (settings.aptosInitEnabled) {
//                aptosCli.init(
//                    project, module,
//                    privateKeyPath = settings.initData.privateKeyPath,
//                    faucetUrl = settings.initData.faucetUrl,
//                    restUrl = settings.initData.restUrl,
//                )
//                    .unwrapOrThrow()
//            }
            manifestFile
        }


        project.moveSettings.modify {
            it.aptosPath = projectConfig.aptosSettings.aptosPath
        }
        project.addDefaultBuildRunConfiguration(isSelected = true)
        project.openFile(manifestFile)

        updateAllNotifications(project)
        project.moveProjects.refreshAllProjects()
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<AptosProjectConfig>,
        callback: AbstractNewProjectStep.AbstractCallback<AptosProjectConfig>
    ): AbstractActionWithPanel =
        AptosProjectConfigStep(projectGenerator)
}
