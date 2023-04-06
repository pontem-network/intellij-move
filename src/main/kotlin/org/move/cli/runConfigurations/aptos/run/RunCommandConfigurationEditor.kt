package org.move.cli.runConfigurations.aptos.run

import com.intellij.openapi.project.Project
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationEditorBase
import org.move.lang.core.psi.MvFunction
import org.move.lang.index.MvEntryFunctionIndex
import java.nio.file.Path

class RunCommandConfigurationEditor(
    project: Project,
    command: String,
    workingDirectory: Path?,
) :
    FunctionCallConfigurationEditorBase<RunCommandConfiguration>(project, command, workingDirectory) {

    override fun getFunctionCompletionVariants(project: Project): Collection<String> {
        return MvEntryFunctionIndex.getAllKeysForCompletion(project)
    }

    override fun getFunction(project: Project, functionId: String): MvFunction? {
        return RunCommandConfiguration.getEntryFunction(project, functionId)
    }

    override fun generateCommand(): String {
        val commandLine = functionCall.toAptosCommandLine("move run")
            ?: error("Cannot generate command")
        return commandLine.joinedCommand()
    }
}
