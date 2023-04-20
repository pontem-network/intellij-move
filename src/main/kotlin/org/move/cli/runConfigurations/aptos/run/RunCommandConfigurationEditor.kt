package org.move.cli.runConfigurations.aptos.run

import com.intellij.openapi.project.Project
import org.move.cli.MoveProject
import org.move.cli.runConfigurations.aptos.FunctionCallConfigurationEditorBase
import org.move.lang.core.psi.MvFunction
import org.move.lang.index.MvFunctionIndex
import org.move.stdext.RsResult

class RunCommandConfigurationEditor(
    project: Project,
    command: String,
    moveProject: MoveProject?,
) :
    FunctionCallConfigurationEditorBase<RunCommandConfiguration>(project, command, moveProject, "move run") {

    override fun getFunctionCompletionVariants(project: Project): Collection<String> {
        return MvFunctionIndex.getAllKeysForCompletion(project)
    }

    override fun getFunction(moveProject: MoveProject, functionId: String): MvFunction? {
        return RunCommandConfiguration.getEntryFunction(moveProject, functionId)
    }
}

fun String.quoted(): String = "\"$this\""
