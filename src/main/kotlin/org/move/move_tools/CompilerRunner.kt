package org.move.move_tools

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner

class CompilerRunner : ProgramRunner<RunnerSettings> {
    override fun getRunnerId(): String = RUNNER_ID

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId == DefaultRunExecutor.EXECUTOR_ID
                && profile is CompilerCheckConfiguration
    }

    override fun execute(environment: ExecutionEnvironment) {
        val state = environment.state ?: return

        TODO("Not yet implemented")
    }

    companion object {
        const val RUNNER_ID: String = "CompilerRunner"
    }
}