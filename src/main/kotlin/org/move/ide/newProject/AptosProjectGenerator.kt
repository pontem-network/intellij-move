package org.move.ide.newProject

import com.intellij.ide.util.projectWizard.AbstractNewProjectStep
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel
import com.intellij.platform.DirectoryProjectGenerator
import com.intellij.platform.DirectoryProjectGeneratorBase
import com.intellij.platform.ProjectGeneratorPeer
import org.move.cli.PluginApplicationDisposable
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.addDefaultBuildRunConfiguration
import org.move.cli.settings.AptosSettingsPanel
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.computeWithCancelableProgress
import org.move.stdext.unwrapOrThrow

data class AptosProjectConfig(
    val panelData: AptosSettingsPanel.PanelData,
)

class AptosProjectGenerator: DirectoryProjectGeneratorBase<AptosProjectConfig>(),
                             CustomStepProjectGenerator<AptosProjectConfig> {

    private val disposable = service<PluginApplicationDisposable>()

    override fun getName() = "Aptos"
    override fun getLogo() = MoveIcons.APTOS_LOGO
    override fun createPeer(): ProjectGeneratorPeer<AptosProjectConfig> = AptosProjectGeneratorPeer(disposable)

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        projectConfig: AptosProjectConfig,
        module: Module
    ) {
        val aptosExecutor = projectConfig.panelData.aptosExec.toExecutor() ?: return
        val packageName = project.name

        val manifestFile =
            project.computeWithCancelableProgress("Generating Aptos project...") {
                val manifestFile = aptosExecutor.moveInit(
                    project,
                    disposable,
                    rootDirectory = baseDir,
                    packageName = packageName
                )
                    .unwrapOrThrow() // TODO throw? really??

                manifestFile
            }


        project.moveSettings.modify {
            it.aptosPath = projectConfig.panelData.aptosExec.pathToSettingsFormat()
        }
        project.addDefaultBuildRunConfiguration(isSelected = true)
        project.openFile(manifestFile)

        updateAllNotifications(project)
        project.moveProjectsService.scheduleProjectsRefresh()
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<AptosProjectConfig>,
        callback: AbstractNewProjectStep.AbstractCallback<AptosProjectConfig>
    ): AbstractActionWithPanel =
        AptosProjectConfigStep(projectGenerator)
}
