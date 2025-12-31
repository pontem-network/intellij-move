package org.move.cli.runConfigurations

import com.intellij.execution.configuration.EnvironmentVariablesData
import org.move.cli.runConfigurations.endless.EndlessArgs
import org.move.cli.tools.MvCommandLine
import java.nio.file.Path

class EndlessCommandLine(
    val subCommand: String?,
    arguments: EndlessArgs = EndlessArgs(),
    workingDirectory: Path? = null,
    environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
): MvCommandLine(
    subCommand?.split(" ").orEmpty() + arguments.args,
    workingDirectory,
    environmentVariables
)
