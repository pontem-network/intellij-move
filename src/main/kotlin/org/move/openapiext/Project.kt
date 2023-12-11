package org.move.openapiext

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfiguration

val Project.runManager: RunManager get() = RunManager.getInstance(this)

fun Project.aptosCommandConfigurations(): List<AnyCommandConfiguration> =
    runManager.allConfigurationsList
        .filterIsInstance<AnyCommandConfiguration>()

fun Project.aptosCommandConfigurationsSettings(): List<RunnerAndConfigurationSettings> =
    runManager.allSettings
        .filter { it.configuration is AnyCommandConfiguration }

//fun Project.aptosBuildRunConfigurations(): List<MoveCommandConfiguration> =
//    aptosCommandConfigurations().filter { it.command.startsWith("move compile") }

inline fun <reified T : Configurable> Project.showSettings() {
    ShowSettingsUtil.getInstance().showSettingsDialog(this, T::class.java)
}
