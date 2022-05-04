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
import org.move.cli.MoveConstants
import org.move.cli.moveProjects
import org.move.cli.runconfig.addDefaultBuildRunConfiguration
import org.move.cli.settings.AptosSettingsPanel
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.ide.notifications.updateAllNotifications
import org.move.lang.hasChild
import org.move.openapiext.computeWithCancelableProgress
import org.move.stdext.unwrapOrThrow

typealias ConfigurationData = AptosSettingsPanel.Data

class MvDirectoryProjectGenerator : DirectoryProjectGeneratorBase<ConfigurationData>(),
                                    CustomStepProjectGenerator<ConfigurationData> {

    private var peer: MvProjectGeneratorPeer? = null

    override fun getName() = "Move"
    override fun getLogo() = MoveIcons.MOVE
    override fun createPeer(): ProjectGeneratorPeer<ConfigurationData> =
        MvProjectGeneratorPeer().also { peer = it }

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        settings: ConfigurationData,
        module: Module
    ) {
        val aptos = settings.aptos() ?: return
        val packageName = project.name

        val manifestFile = project.computeWithCancelableProgress("Generating Aptos project...") {
            aptos.move_init(
                project, module,
                rootDirectory = baseDir,
                packageName = packageName
            )
                .unwrapOrThrow() // TODO throw? really??
        }

        // TODO: add `aptos config` step

        project.moveSettings.modify {
            it.aptosPath = settings.aptosPath
        }
        project.addDefaultBuildRunConfiguration(isSelected = true)
        project.openFile(manifestFile)

        updateAllNotifications(project)
        project.moveProjects.refreshAllProjects()
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<AptosSettingsPanel.Data>,
        callback: AbstractNewProjectStep.AbstractCallback<AptosSettingsPanel.Data>
    ): AbstractActionWithPanel = MvProjectSettingsStep(projectGenerator)
}
