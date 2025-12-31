package org.move.cli.runConfigurations.endless

import org.move.cli.MoveProject
import org.move.cli.externalLinter.ExternalLinter
import org.move.cli.externalLinter.externalLinterSettings
import org.move.cli.settings.moveSettings
import java.nio.file.Path

data class EndlessExternalLinterArgs(
    val linter: ExternalLinter,
    val moveProjectDirectory: Path,
    val extraArguments: String,
    val envs: Map<String, String>,
    val enableMove2: Boolean,
//    val skipLatestGitDeps: Boolean,
) {
    companion object {
        fun forMoveProject(moveProject: MoveProject): EndlessExternalLinterArgs {
            val linterSettings = moveProject.project.externalLinterSettings
            val moveSettings = moveProject.project.moveSettings

            val additionalArguments = linterSettings.additionalArguments
            val enviroment = linterSettings.envs
            val workingDirectory = moveProject.workingDirectory

            return EndlessExternalLinterArgs(
                linterSettings.tool,
                workingDirectory,
                additionalArguments,
                enviroment,
                enableMove2 = moveSettings.enableMove2,
//                skipLatestGitDeps = moveSettings.skipFetchLatestGitDeps
            )
        }
    }
}
