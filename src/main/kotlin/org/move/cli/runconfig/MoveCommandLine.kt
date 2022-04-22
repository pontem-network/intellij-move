package org.move.cli.runconfig

import com.intellij.execution.configuration.EnvironmentVariablesData
import java.nio.file.Path

data class MoveCommandLine(
    val command: String,
    val workingDirectory: Path?,
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
)
