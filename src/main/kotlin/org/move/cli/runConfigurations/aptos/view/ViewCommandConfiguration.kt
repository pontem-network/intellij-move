package org.move.cli.runConfigurations.aptos.view

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationBase
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationEditor

class ViewCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory
) : FunctionCallConfigurationBase(project, factory, ViewCommandConfigurationHandler()) {

    override fun getConfigurationEditor(): FunctionCallConfigurationEditor<ViewCommandConfiguration> {
        val moveProject = project.moveProjectsService.allProjects.first()
        val editor = FunctionCallConfigurationEditor<ViewCommandConfiguration>(
            ViewCommandConfigurationHandler(),
            moveProject,
        )
        return editor
    }
}
