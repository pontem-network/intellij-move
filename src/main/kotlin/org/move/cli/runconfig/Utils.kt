package org.move.cli.runconfig

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import org.move.stdext.toPath

//fun RunManager.createAptosCommandRunConfiguration(
//    aptosCommandLine: AptosCommandLine,
//    name: String? = null
//): RunnerAndConfigurationSettings {
//    val runnerAndConfigurationSettings = createConfiguration(
//        name ?: aptosCommandLine.command,
//        AptosCommandConfigurationType::class.java
//    )
//    val configuration = runnerAndConfigurationSettings.configuration as AptosCommandRunConfiguration
//    configuration.setFromCmd(aptosCommandLine)
//    return runnerAndConfigurationSettings
//}

fun Project.makeDefaultBuildRunConfiguration(): AptosCommandConfiguration {
    val runManager = RunManager.getInstance(this)
    val configurationFactory = DefaultRunConfigurationFactory(runManager, this)
    val configuration = configurationFactory.createAptosBuildConfiguration()

    runManager.addConfiguration(configuration)
    runManager.selectedConfiguration = configuration
    return configuration.configuration as AptosCommandConfiguration
}

private class DefaultRunConfigurationFactory(val runManager: RunManager, val project: Project) {
    private val aptosProjectName = project.name.replace(' ', '_')

    fun createAptosBuildConfiguration(): RunnerAndConfigurationSettings =
        runManager.createConfiguration("Build $aptosProjectName", AptosCommandConfigurationType::class.java)
            .apply {
                (configuration as? AptosCommandConfiguration)?.apply {
                    command = "move compile"
                    workingDirectory = project.basePath?.toPath()
                }
            }
}
