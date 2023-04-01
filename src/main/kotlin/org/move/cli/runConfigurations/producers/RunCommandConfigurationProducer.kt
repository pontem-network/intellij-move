package org.move.cli.runConfigurations.producers

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.aptos.AptosCommandLine
import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationFactory
import org.move.lang.core.psi.MvFunction
import org.move.lang.core.psi.ext.isEntry

class RunCommandConfigurationProducer : CommandConfigurationProducerBase() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        RunCommandConfigurationFactory(AptosConfigurationType.getInstance())

    override fun configFromLocation(location: PsiElement) = fromLocation(location)

    companion object {
        fun fromLocation(location: PsiElement): CommandLineFromContext? {
            val entryFunction =
                findElement<MvFunction>(location, true)?.takeIf { it.isEntry } ?: return null

            val commandLine = AptosCommandLine(
                "move run",
                arguments = listOf("--function-id", entryFunction.qualName.cmdText()),
            )
            return CommandLineFromContext(
                entryFunction,
                "Run ${entryFunction.qualName.shortCmdText()}",
                commandLine
            )
        }
    }
}
