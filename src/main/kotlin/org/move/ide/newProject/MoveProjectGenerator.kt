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
import org.move.cli.runConfigurations.endless.Endless
import org.move.cli.settings.endless.EndlessExecType
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.openapiext.computeWithCancelableProgress
import org.move.openapiext.openFileInEditor
import org.move.stdext.unwrapOrThrow

data class EndlessProjectConfig(val endlessPath: String?)

class MoveProjectGenerator: DirectoryProjectGeneratorBase<EndlessProjectConfig>(),
                            CustomStepProjectGenerator<EndlessProjectConfig> {

    private val disposable = service<PluginApplicationDisposable>()

    override fun getName() = "Endless"
    override fun getLogo() = MoveIcons.ENDLESS_LOGO
    override fun createPeer(): ProjectGeneratorPeer<EndlessProjectConfig> = MoveProjectGeneratorPeer(disposable)

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        projectConfig: EndlessProjectConfig,
        module: Module
    ) {
        val packageName = project.name
        val endlessPath =
            EndlessExecType.endlessCliPath(projectConfig.endlessPath)
                ?: error("validated before")
        val endless = Endless(endlessPath, disposable)
        val moveTomlFile =
            project.computeWithCancelableProgress("Generating Endless project...") {
                val manifestFile =
                    endless.init(
                        project,
                        rootDirectory = baseDir,
                        packageName = packageName
                    )
                        .unwrapOrThrow() // TODO throw? really??
                manifestFile
            }
        // update settings (and refresh Endless projects too)
        project.moveSettings.modify {
            it.endlessPath = projectConfig.endlessPath
        }

        // NOTE:
        // this cannot be moved to a ProjectActivity, as Move.toml files
        // are not created by the time those activities are executed
        project.openFileInEditor(moveTomlFile)

        project.moveProjectsService.scheduleProjectsRefresh("After `endless move init`")
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<EndlessProjectConfig>,
        callback: AbstractNewProjectStep.AbstractCallback<EndlessProjectConfig>
    ): AbstractActionWithPanel =
        ConfigStep(projectGenerator)

    class ConfigStep(generator: DirectoryProjectGenerator<EndlessProjectConfig>):
        ProjectSettingsStepBase<EndlessProjectConfig>(
            generator,
            AbstractNewProjectStep.AbstractCallback()
        )

}
