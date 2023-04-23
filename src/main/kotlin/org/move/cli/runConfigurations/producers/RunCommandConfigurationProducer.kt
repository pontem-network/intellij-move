package org.move.cli.runConfigurations.producers

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.psi.PsiElement
import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.run.RunCommandConfiguration
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationFactory
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationHandler

class RunCommandConfigurationProducer : FunctionCallConfigurationProducerBase<RunCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        RunCommandConfigurationFactory(AptosConfigurationType.getInstance())

    override fun configFromLocation(location: PsiElement) =
        RunCommandConfigurationHandler().configurationFromLocation(location)

//    companion object {
//        fun fromLocation(location: PsiElement): CommandLineFromContext? {
//            val entryFunction =
//                findElement<MvFunction>(location, true)?.takeIf { it.isEntry } ?: return null
//            val functionId = entryFunction.functionId() ?: return null
//
//            val moveProject = entryFunction.moveProject ?: return null
//            val profileName = moveProject.profiles.firstOrNull()
//            val workingDirectory = moveProject.contentRootPath
//
//            val arguments = mutableListOf<String>()
//            if (profileName != null) {
//                arguments.addAll(listOf("--profile", profileName))
//            }
//            arguments.addAll(listOf("--function-id", functionId))
//
//            val commandLine = AptosCommandLine("move run", arguments, workingDirectory)
//            return CommandLineFromContext(
//                entryFunction,
//                "Run ${ItemQualName.qualNameForCompletion(functionId)}",
//                commandLine
//            )
//        }
//    }
}
