package org.move.openapiext

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.runConfigurations.aptos.cmd.AptosCommandConfiguration
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.common.isHeadlessEnvironment

val Project.runManager: RunManager get() = RunManager.getInstance(this)

fun Project.aptosCommandConfigurations(): List<AptosCommandConfiguration> =
    runManager.allConfigurationsList
        .filterIsInstance<AptosCommandConfiguration>()

fun Project.aptosCommandConfigurationsSettings(): List<RunnerAndConfigurationSettings> =
    runManager.allSettings
        .filter { it.configuration is AptosCommandConfiguration }

//fun Project.aptosBuildRunConfigurations(): List<MoveCommandConfiguration> =
//    aptosCommandConfigurations().filter { it.command.startsWith("move compile") }

inline fun <reified T: Configurable> Project.showSettingsDialog() {
    ShowSettingsUtil.getInstance().showSettingsDialog(this, T::class.java)
}

fun Project.addRunConfiguration(
    isSelected: Boolean = false,
    configurationFactory: (RunManager, Project) -> RunnerAndConfigurationSettings,
): RunnerAndConfigurationSettings {
    val runManager = RunManager.getInstance(this)
    val runnerAndConfigurationSettings = configurationFactory(runManager, this)
    runManager.addConfiguration(runnerAndConfigurationSettings)
    if (isSelected) {
        runManager.selectedConfiguration = runnerAndConfigurationSettings
    }
    return runnerAndConfigurationSettings
}


fun Project.openFileInEditor(file: VirtualFile, requestFocus: Boolean = true) {
    if (!isHeadlessEnvironment) {
        val navigatable = PsiNavigationSupport.getInstance().createNavigatable(this, file, -1)
        navigatable.navigate(requestFocus)
    }
}

