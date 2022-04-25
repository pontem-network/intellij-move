package org.move.cli.runconfig

import com.intellij.execution.configuration.EnvironmentVariablesData
import java.nio.file.Path

data class MoveCmdConf(
    val name: String,
    val command: String,
    val workingDirectory: Path,
    val env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) {
    fun commandLine(): MoveCmd {
        return MoveCmd(command, workingDirectory, env)
    }
}

data class MoveCmd(
    val command: String,
    val workingDirectory: Path?,
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
)
