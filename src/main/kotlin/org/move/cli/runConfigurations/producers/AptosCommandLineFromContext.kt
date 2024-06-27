package org.move.cli.runConfigurations.producers

import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.AptosCommandLine

data class AptosCommandLineFromContext(
    val sourceElement: PsiElement,
    val configurationName: String,
    val commandLine: AptosCommandLine
)
