package org.move.cli.runConfigurations.producers

import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.EndlessCommandLine

data class EndlessCommandLineFromContext(
    val sourceElement: PsiElement,
    val configurationName: String,
    val commandLine: EndlessCommandLine
)
