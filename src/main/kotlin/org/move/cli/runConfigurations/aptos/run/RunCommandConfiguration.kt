package org.move.cli.runConfigurations.aptos.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationBase
import org.move.lang.core.psi.MvFunction
import org.move.lang.index.MvEntryFunctionIndex

class RunCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory
) : FunctionCallConfigurationBase(project, factory) {

    override fun getFunction(project: Project, functionId: String): MvFunction? =
        getEntryFunction(project, functionId)

    override fun getConfigurationEditor(): RunCommandConfigurationEditor {
        return RunCommandConfigurationEditor(project, command, workingDirectory)
    }

    companion object {
        fun getEntryFunction(project: Project, functionId: String): MvFunction? {
            return MvEntryFunctionIndex.getEntryFunction(project, functionId)
        }
    }
}
