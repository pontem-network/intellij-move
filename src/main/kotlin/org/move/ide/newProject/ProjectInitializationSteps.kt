package org.move.ide.newProject

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.move.cli.Consts
import org.move.cli.runConfigurations.aptos.AptosTransactionConfigurationType
import org.move.cli.runConfigurations.aptos.cmd.AptosCommandConfiguration
import org.move.cli.runConfigurations.aptos.cmd.AptosCommandConfigurationFactory
import org.move.ide.notifications.updateAllNotifications
import org.move.openapiext.addRunConfiguration
import org.move.openapiext.contentRoots
import org.move.openapiext.openFile
import org.move.openapiext.runManager
import org.move.stdext.toPath

object ProjectInitializationSteps {
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
                val configurationFactory = AptosTransactionConfigurationType.getInstance()
                    .configurationFactories.find { it is AptosCommandConfigurationFactory }
                    ?: error("AnyCommandConfigurationFactory should be present in the factories list")
                runManager.createConfiguration("Compile Project", configurationFactory)
                    .apply {
                        (configuration as? AptosCommandConfiguration)?.apply {
                            command = "move compile"
                            workingDirectory = project.basePath?.toPath()
                        }
                    }
            }
        return runConfigurationAndWithSettings
    }
}

