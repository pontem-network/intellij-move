package org.move.cli.runConfigurations.sui

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.CommandConfigurationBase
import org.move.cli.settings.moveSettings
import org.move.cli.settings.suiExecPath
import org.move.stdext.toPathOrNull
import java.nio.file.Path

class SuiCommandConfiguration(
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

    override fun getCliPath(project: Project): Path? = project.suiExecPath

    override fun getConfigurationEditor() = SuiCommandConfigurationEditor()
}
