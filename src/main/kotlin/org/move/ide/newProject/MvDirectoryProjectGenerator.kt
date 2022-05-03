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
import org.move.cli.settings.MoveProjectSettingsPanel
import org.move.cli.settings.moveSettings
import org.move.ide.MoveIcons
import org.move.openapiext.computeWithCancelableProgress
import org.move.stdext.unwrapOrThrow

typealias ConfigurationData = MoveProjectSettingsPanel.Data

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
        val projectName = project.name
        val generatedFiles = project.computeWithCancelableProgress("Generating Aptos project...") {
            aptos.init(project, module, baseDir, projectName).unwrapOrThrow() // TODO throw? really??
        }

        project.moveSettings.modify {
            it.aptosPath = settings.aptosPath
        }
        project.openFiles(generatedFiles)

        // TODO: add `aptos config` step
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<MoveProjectSettingsPanel.Data>,
        callback: AbstractNewProjectStep.AbstractCallback<MoveProjectSettingsPanel.Data>
    ): AbstractActionWithPanel = MvProjectSettingsStep(projectGenerator)
}
