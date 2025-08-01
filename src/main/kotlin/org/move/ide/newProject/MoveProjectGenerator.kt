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
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.aptos.Aptos
import org.move.cli.settings.aptos.AptosExecType
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.openapiext.computeWithCancelableProgress
import org.move.openapiext.openFileInEditor
import org.move.stdext.unwrapOrThrow

data class AptosProjectConfig(val aptosPath: String?)

class MoveProjectGenerator: DirectoryProjectGeneratorBase<AptosProjectConfig>(),
                            CustomStepProjectGenerator<AptosProjectConfig> {

    private val disposable = service<PluginApplicationDisposable>()

    override fun getName() = "Aptos"
    override fun getLogo() = MoveIcons.APTOS_LOGO
    override fun createPeer(): ProjectGeneratorPeer<AptosProjectConfig> = MoveProjectGeneratorPeer(disposable)

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        projectConfig: AptosProjectConfig,
        module: Module
    ) {
        val packageName = project.name
        val aptosPath =
            AptosExecType.aptosCliPath(projectConfig.aptosPath)
                ?: error("validated before")
        val aptos = Aptos(aptosPath, disposable)
        val moveTomlFile =
            project.computeWithCancelableProgress("Generating Aptos project...") {
                val manifestFile =
                    aptos.init(
                        project,
                        rootDirectory = baseDir,
                        packageName = packageName
                    )
                        .unwrapOrThrow() // TODO throw? really??
                manifestFile
            }
        // update settings (and refresh Aptos projects too)
        project.moveSettings.modify {
            it.aptosPath = projectConfig.aptosPath
        }

        // NOTE:
        // this cannot be moved to a ProjectActivity, as Move.toml files
        // are not created by the time those activities are executed
        project.openFileInEditor(moveTomlFile)

        project.moveProjectsService.scheduleProjectsRefresh("After `aptos move init`")
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<AptosProjectConfig>,
        callback: AbstractNewProjectStep.AbstractCallback<AptosProjectConfig>
    ): AbstractActionWithPanel =
        ConfigStep(projectGenerator)

    class ConfigStep(generator: DirectoryProjectGenerator<AptosProjectConfig>):
        ProjectSettingsStepBase<AptosProjectConfig>(
            generator,
            AbstractNewProjectStep.AbstractCallback()
        )

}
