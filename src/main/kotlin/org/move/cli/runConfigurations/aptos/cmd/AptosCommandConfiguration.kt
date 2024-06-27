package org.move.cli.runConfigurations.aptos.cmd

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.settings.aptosExecPath
import java.nio.file.Path

class AptosCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory
):
    CommandConfigurationBase(project, factory) {

    init {
        workingDirectory = if (!project.isDefault) {
            project.moveProjectsService.allProjects.firstOrNull()?.contentRootPath
        } else {
            null
        }
    }

    override fun getCliPath(project: Project): Path? = project.aptosExecPath

    override fun getConfigurationEditor() = AptosCommandConfigurationEditor()
}
