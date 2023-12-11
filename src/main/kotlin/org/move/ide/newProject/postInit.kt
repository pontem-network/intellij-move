package org.move.ide.newProject

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.Consts
import org.move.cli.runConfigurations.aptos.AptosConfigurationType
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfiguration
import org.move.cli.runConfigurations.aptos.any.AnyCommandConfigurationFactory
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.contentRoots
import org.move.openapiext.runManager
import org.move.stdext.toPath

object ProjectInitialization {
    fun openMoveTomlInEditor(project: Project, moveTomlFile: VirtualFile? = null) {
        val file =
            moveTomlFile ?: run {
                val packageRoot = project.contentRoots.firstOrNull()
                if (packageRoot != null) {
                    val manifest = packageRoot.findChild(Consts.MANIFEST_FILE)
                    return@run manifest
                }
                return@run null
            }
        if (file != null) {
            project.openFile(file)
        }
        updateAllNotifications(project)
    }

    fun createDefaultCompileConfigurationIfNotExists(project: Project) {
        if (project.runManager.allConfigurationsList.isEmpty()) {
            createDefaultCompileConfiguration(project, true)
        }
    }

    fun createDefaultCompileConfiguration(project: Project, selected: Boolean): RunnerAndConfigurationSettings {
        val runConfigurationAndWithSettings =
            project.addRunConfiguration(selected) { runManager, _ ->
                val configurationFactory = AptosConfigurationType.getInstance()
                    .configurationFactories.find { it is AnyCommandConfigurationFactory }
                    ?: error("AnyCommandConfigurationFactory should be present in the factories list")
                runManager.createConfiguration("Compile Project", configurationFactory)
                    .apply {
                        (configuration as? AnyCommandConfiguration)?.apply {
                            command = "move compile"
                            workingDirectory = project.basePath?.toPath()
                        }
                    }
            }
        return runConfigurationAndWithSettings
    }
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

