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
import org.move.cli.runConfigurations.InitProjectCli
import org.move.cli.settings.aptos.AptosExec
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.openapiext.computeWithCancelableProgress
import org.move.stdext.toPathOrNull
import org.move.stdext.unwrapOrThrow

data class AptosProjectConfig(val aptosExec: AptosExec)

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
        val packageName = project.name
        val aptosPath = projectConfig.aptosExec.execPath.toPathOrNull() ?: return
        val aptosInitializer = InitProjectCli.Aptos(aptosPath)

        val manifestFile =
            project.computeWithCancelableProgress("Generating Aptos project...") {
                val manifestFile =
                    aptosInitializer.init(
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
            it.aptosPath = projectConfig.aptosExec.pathToSettingsFormat()
        }

        ProjectInitialization.createDefaultCompileConfigurationIfNotExists(project)
        ProjectInitialization.openMoveTomlInEditor(project, manifestFile)
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
