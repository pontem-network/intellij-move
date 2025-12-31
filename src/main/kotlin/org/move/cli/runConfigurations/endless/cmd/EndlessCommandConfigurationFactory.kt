package org.move.cli.runConfigurations.endless.cmd

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class EndlessCommandConfigurationFactory(
    configurationType: ConfigurationType
):
    ConfigurationFactory(configurationType) {

    override fun getId(): String = "AnyCommand"

    override fun getName(): String = "any command"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return EndlessCommandConfiguration(project, this)
    }

//    companion object {
//        fun createTemplateRunConfiguration(
//            project: Project,
//            configurationName: String,
//            save: Boolean
//        ): RunnerAndConfigurationSettings {
//            val runManager = RunManagerEx.getInstanceEx(project)
//            val runConfiguration = runManager.createConfiguration(
//                configurationName,
//                EndlessCommandConfigurationFactory(EndlessTransactionConfigurationType.getInstance())
//            )
//            if (save) {
//                runManager.setTemporaryConfiguration(runConfiguration)
//            }
//            return runConfiguration
//        }
//    }
}
