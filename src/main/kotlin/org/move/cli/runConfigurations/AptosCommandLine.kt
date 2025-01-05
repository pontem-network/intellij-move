package org.move.cli.runConfigurations

import com.intellij.execution.configuration.EnvironmentVariablesData
import org.move.cli.tools.MvCommandLine
import java.nio.file.Path

class AptosCommandLine(
    subCommand: String?,
    arguments: List<String> = emptyList(),
    workingDirectory: Path? = null,
    environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
): MvCommandLine(
    subCommand,
    arguments,
    workingDirectory,
    environmentVariables
) {
    fun copy(
        subCommand: String? = this.subCommand,
        arguments: List<String> = this.arguments,
    ): AptosCommandLine {
        return AptosCommandLine(
            subCommand,
            arguments,
            this.workingDirectory,
            this.environmentVariables
        )
    }
}
