package org.move.cli.runConfigurations.producers

import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.CliCommandLineArgs

data class CommandLineArgsFromContext(
    val sourceElement: PsiElement,
    val configurationName: String,
    val commandLineArgs: CliCommandLineArgs
)
