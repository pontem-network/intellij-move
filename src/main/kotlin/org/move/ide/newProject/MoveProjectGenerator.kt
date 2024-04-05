package org.move.ide.newProject

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import org.move.cli.PluginApplicationDisposable
import org.move.cli.runConfigurations.BlockchainCli
import org.move.cli.settings.Blockchain
import org.move.cli.settings.Blockchain.APTOS
import org.move.cli.settings.Blockchain.SUI
import org.move.cli.settings.aptos.AptosExecType
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.openapiext.computeWithCancelableProgress
import org.move.stdext.toPathOrNull
import org.move.stdext.unwrapOrThrow

data class MoveProjectConfig(
    val blockchain: Blockchain,
    val aptosExecType: AptosExecType,
    val localAptosPath: String?,
    val localSuiPath: String?
)

class MoveProjectGenerator: DirectoryProjectGeneratorBase<MoveProjectConfig>(),
                            CustomStepProjectGenerator<MoveProjectConfig> {

    private val disposable = service<PluginApplicationDisposable>()

    override fun getName() = "Move"
    override fun getLogo() = MoveIcons.MOVE_LOGO
    override fun createPeer(): ProjectGeneratorPeer<MoveProjectConfig> = MoveProjectGeneratorPeer(disposable)

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        projectConfig: MoveProjectConfig,
        module: Module
    ) {
        val packageName = project.name
        val blockchain = projectConfig.blockchain
        val projectCli =
            when (blockchain) {
                APTOS -> {
                    val aptosPath =
                        AptosExecType.aptosExecPath(projectConfig.aptosExecType, projectConfig.localAptosPath)
                            ?: error("validated before")
                    BlockchainCli.Aptos(aptosPath)
                }
                SUI -> {
                    val suiPath = projectConfig.localSuiPath?.toPathOrNull() ?: error("validated before")
                    BlockchainCli.Sui(suiPath)
                }
            }
        val manifestFile =
            project.computeWithCancelableProgress("Generating $blockchain project...") {
                val manifestFile =
                    projectCli.init(
                        project,
                        disposable,
                        rootDirectory = baseDir,
                        packageName = packageName
                    )
                        .unwrapOrThrow() // TODO throw? really??
                manifestFile
            }
        // update settings (and refresh Aptos projects too)
        project.moveSettings.modify {
            it.blockchain = blockchain
            when (projectCli) {
                is BlockchainCli.Aptos -> {
                    it.aptosExecType = projectConfig.aptosExecType
                    it.localAptosPath = projectConfig.localAptosPath
                }
                is BlockchainCli.Sui -> {
                    it.localSuiPath = projectConfig.localSuiPath
                }
            }
        }
        ProjectInitializationSteps.createDefaultCompileConfigurationIfNotExists(project)
        ProjectInitializationSteps.openMoveTomlInEditor(project, manifestFile)
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<MoveProjectConfig>,
        callback: AbstractNewProjectStep.AbstractCallback<MoveProjectConfig>
    ): AbstractActionWithPanel =
        ConfigStep(projectGenerator)

    class ConfigStep(generator: DirectoryProjectGenerator<MoveProjectConfig>):
        ProjectSettingsStepBase<MoveProjectConfig>(
            generator,
            AbstractNewProjectStep.AbstractCallback()
        )

}
