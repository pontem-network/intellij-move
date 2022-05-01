package org.move.cli.runconfig

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.psi.PsiElement
import java.nio.file.Path

data class AptosCommandConf(
    val sourceElement: PsiElement,
    val configurationName: String,
    val commandLine: AptosCommandLine
) {
}

data class AptosCommandLine(
    val command: String,
    val workingDirectory: Path?,
    val environmentVariables: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
)
