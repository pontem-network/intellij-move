package org.move.cli.runConfigurations

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.legacy.MoveCommandConfiguration
import org.move.stdext.toPath

private val LOG = logger<Project>()

fun Project.addDefaultBuildRunConfiguration(isSelected: Boolean = false): RunnerAndConfigurationSettings {
    val runManager = RunManager.getInstance(this)
    val configurationFactory = DefaultRunConfigurationFactory(runManager, this)
    val configuration = configurationFactory.createAptosBuildConfiguration()

    runManager.addConfiguration(configuration)
    LOG.info("Default \"Build\" run configuration is added")
    if (isSelected) {
        runManager.selectedConfiguration = configuration
    }
    return configuration
}

private class DefaultRunConfigurationFactory(val runManager: RunManager, val project: Project) {
//    private val aptosProjectName = project.name.replace(' ', '_')

    fun createAptosBuildConfiguration(): RunnerAndConfigurationSettings =
        runManager.createConfiguration("Build", AptosConfigurationType::class.java)
            .apply {
                (configuration as? MoveCommandConfiguration)?.apply {
                    command = "move compile"
                    workingDirectory = project.basePath?.toPath()
                }
            }
}
