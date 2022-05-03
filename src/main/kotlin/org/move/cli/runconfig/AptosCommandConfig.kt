package org.move.cli.runconfig

import com.intellij.psi.PsiElement
import org.move.cli.AptosCommandLine

data class AptosCommandConfig(
    val sourceElement: PsiElement,
    val configurationName: String,
    val commandLine: AptosCommandLine
) {
}
