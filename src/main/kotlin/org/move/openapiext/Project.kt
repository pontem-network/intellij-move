package org.move.openapiext

import com.intellij.execution.RunManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import org.move.cli.runConfigurations.legacy.MoveCommandConfiguration

val Project.runManager: RunManager get() = RunManager.getInstance(this)

fun Project.aptosRunConfigurations(): List<MoveCommandConfiguration> =
    runManager.allConfigurationsList
        .filterIsInstance<MoveCommandConfiguration>()

fun Project.aptosBuildRunConfigurations(): List<MoveCommandConfiguration> =
    aptosRunConfigurations().filter { it.command.startsWith("move compile") }

inline fun <reified T : Configurable> Project.showSettings() {
    ShowSettingsUtil.getInstance().showSettingsDialog(this, T::class.java)
}
