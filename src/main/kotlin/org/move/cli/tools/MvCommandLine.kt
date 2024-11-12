package org.move.cli.tools

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.util.execution.ParametersListUtil
import java.nio.file.Path

open class MvCommandLine(
    val arguments: List<String> = emptyList(),
    val workingDirectory: Path? = null,
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) {
    val commandLineString: String get() = ParametersListUtil.join(arguments)

    fun toGeneralCommandLine(cliExePath: Path, emulateTerminal: Boolean = false): GeneralCommandLine {
        var commandLine = GeneralCommandLine()
            .withExePath(cliExePath.toString())
            .withParameters(this.arguments)
            .withWorkingDirectory(this.workingDirectory)
            .withCharset(Charsets.UTF_8)
            // disables default coloring for stderr
            .withRedirectErrorStream(true)
        this.environmentVariables.configureCommandLine(commandLine, true)
        if (emulateTerminal) {
            commandLine = PtyCommandLine(commandLine)
        }
        return commandLine
    }
}