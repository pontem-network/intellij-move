package org.move.cli.runConfigurations

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.util.text.StringUtil
import java.nio.file.Path

data class AptosCommandLine(
    val subCommand: String?,
    val arguments: List<String> = emptyList(),
    val workingDirectory: Path? = null,
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
) {
    fun joinArgs(): String {
        return StringUtil.join(subCommand?.split(" ").orEmpty() + arguments, " ")
    }

    fun toGeneralCommandLine(cliExePath: Path): GeneralCommandLine {
        val generalCommandLine = GeneralCommandLine()
            .withExePath(cliExePath.toString())
            // subcommand can be null
            .withParameters(subCommand?.split(" ").orEmpty())
            .withParameters(this.arguments)
            .withWorkingDirectory(this.workingDirectory)
            .withCharset(Charsets.UTF_8)
        this.environmentVariables.configureCommandLine(generalCommandLine, true)
        return generalCommandLine
    }

    fun toColoredCommandLine(cliExePath: Path): GeneralCommandLine {
        // preudo-tty emulation makes aptos-cli recognize console as tty and show ANSI colors
        val generalCommandLine = PtyCommandLine()
            .withExePath(cliExePath.toString())
            // subcommand can be null
            .withParameters(subCommand?.split(" ").orEmpty())
            .withParameters(this.arguments)
            .withWorkingDirectory(this.workingDirectory)
            .withCharset(Charsets.UTF_8)
            // disables default coloring for stderr
            .withRedirectErrorStream(true)
        this.environmentVariables.configureCommandLine(generalCommandLine, true)
        return generalCommandLine
    }
}
