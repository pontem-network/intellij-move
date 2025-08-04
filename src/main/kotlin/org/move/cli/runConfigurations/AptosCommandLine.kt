package org.move.cli.runConfigurations

import com.intellij.execution.configuration.EnvironmentVariablesData
import org.move.cli.runConfigurations.aptos.AptosArgs
import org.move.cli.tools.MvCommandLine
import java.nio.file.Path

class AptosCommandLine(
    val subCommand: String?,
    arguments: AptosArgs = AptosArgs(),
    workingDirectory: Path? = null,
    environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
): MvCommandLine(
    subCommand?.split(" ").orEmpty() + arguments.args,
    workingDirectory,
    environmentVariables
)
