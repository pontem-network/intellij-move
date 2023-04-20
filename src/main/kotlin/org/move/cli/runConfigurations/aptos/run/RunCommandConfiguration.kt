package org.move.cli.runConfigurations.aptos.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationBase
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.isEntry
import org.move.lang.core.types.ItemQualName
import org.move.lang.index.MvFunctionIndex

class RunCommandConfiguration(
    project: Project,
    factory: ConfigurationFactory
) : FunctionCallConfigurationBase(project, factory) {

    override fun getFunction(moveProject: MoveProject, functionId: String): MvFunction? =
        getEntryFunction(moveProject, functionId)

    override fun getConfigurationEditor(): RunCommandConfigurationEditor {
        return RunCommandConfigurationEditor(project, command, moveProject)
    }

    companion object {
        fun getEntryFunction(moveProject: MoveProject, functionId: String): MvFunction? {
            return MvFunctionIndex.getFunctionByFunctionId(
                moveProject,
                functionId,
                itemFilter = { fn -> fn.isEntry }
            )
        }
    }
}
