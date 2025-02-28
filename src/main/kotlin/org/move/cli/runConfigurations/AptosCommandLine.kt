package org.move.cli.runConfigurations

import com.intellij.execution.configuration.EnvironmentVariablesData
import org.move.cli.tools.MvCommandLine
import java.nio.file.Path

class AptosCommandLine(
    val subCommand: String?,
    arguments: List<String> = emptyList(),
    workingDirectory: Path? = null,
    environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
): MvCommandLine(
    subCommand?.split(" ").orEmpty() + arguments,
    workingDirectory,
    environmentVariables
)
