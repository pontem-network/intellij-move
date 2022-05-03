package org.move.cli.runconfig.buildtool

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.RunConfigurationBeforeRunProvider
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.task.*
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.rejectedPromise
import org.jetbrains.concurrency.resolvedPromise
import org.move.cli.moveProjectRoot
import org.move.cli.runconfig.makeDefaultBuildRunConfiguration
import org.move.openapiext.aptosBuildRunConfigurations
import org.move.openapiext.runManager

@Suppress("UnstableApiUsage")
class AptosBuildTaskRunner : ProjectTaskRunner() {
    override fun canRun(projectTask: ProjectTask): Boolean {
        return projectTask is ModuleBuildTask && projectTask.module.moveProjectRoot != null
    }

    override fun run(
        project: Project,
        context: ProjectTaskContext,
        vararg tasks: ProjectTask?
    ): Promise<Result> {
        val buildConfiguration =
            project.aptosBuildRunConfigurations().firstOrNull()
                ?: project.makeDefaultBuildRunConfiguration()
        val configurationSettings =
            project.runManager.findConfigurationByName(buildConfiguration.name) ?: return rejectedPromise()
        project.runManager.selectedConfiguration = configurationSettings

        val environment = ExecutionEnvironmentBuilder.createOrNull(
            DefaultRunExecutor.getRunExecutorInstance(),
            configurationSettings
        )?.build() ?: return rejectedPromise()
        val success = RunConfigurationBeforeRunProvider.doExecuteTask(environment, configurationSettings, null)

        if (success) {
            return resolvedPromise(TaskRunnerResults.SUCCESS)
        } else {
            return resolvedPromise(TaskRunnerResults.FAILURE)
        }
    }

}
