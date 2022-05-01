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
import org.move.ide.MvIcons

typealias ConfigurationData = MoveProjectSettingsPanel.Data

class MvDirectoryProjectGenerator : DirectoryProjectGeneratorBase<ConfigurationData>(),
                                    CustomStepProjectGenerator<ConfigurationData> {

    private var peer: MvProjectGeneratorPeer? = null

    override fun getName() = "Move"
    override fun getLogo() = MvIcons.MOVE
    override fun createPeer(): ProjectGeneratorPeer<ConfigurationData> =
        MvProjectGeneratorPeer().also { peer = it }

    override fun generateProject(
        project: Project,
        baseDir: VirtualFile,
        settings: ConfigurationData,
        module: Module
    ) {
        project.moveSettings.modify {
            it.aptosPath = settings.aptosPath
            it.privateKey = settings.privateKey
        }
        // TODO: add `aptos move init` step
        // TODO: add `aptos config` step
    }

    override fun createStep(
        projectGenerator: DirectoryProjectGenerator<MoveProjectSettingsPanel.Data>,
        callback: AbstractNewProjectStep.AbstractCallback<MoveProjectSettingsPanel.Data>
    ): AbstractActionWithPanel = MvProjectSettingsStep(projectGenerator)
}
