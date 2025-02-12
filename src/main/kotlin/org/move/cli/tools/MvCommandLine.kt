package org.move.cli.tools

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.util.execution.ParametersListUtil
import java.nio.file.Path

open class MvCommandLine(
    val subCommand: String? = null,
    val arguments: List<String> = emptyList(),
    val workingDirectory: Path? = null,
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) {
    private val subCommandWithArguments get() = subCommand?.split(" ").orEmpty() + arguments

    val commandLineString: String get() = ParametersListUtil.join(subCommandWithArguments)

    fun toGeneralCommandLine(cliExePath: Path, emulateTerminal: Boolean = false): GeneralCommandLine {
        val commandLine = if (emulateTerminal) {
            PtyCommandLine()
        } else {
            GeneralCommandLine()
        }
            .withExePath(cliExePath.toString())
            .withParameters(this.subCommandWithArguments)
            .withWorkingDirectory(this.workingDirectory)
            .withCharset(Charsets.UTF_8)
            // disables default coloring for stderr
            .withRedirectErrorStream(true)
        this.environmentVariables.configureCommandLine(commandLine, true)
        return commandLine
    }
}