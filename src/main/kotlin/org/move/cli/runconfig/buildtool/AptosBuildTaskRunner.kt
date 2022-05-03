package org.move.cli.runconfig.buildtool

import com.intellij.execution.RunManager
import com.intellij.task.ModuleBuildTask
import com.intellij.task.ProjectModelBuildTask
import com.intellij.task.ProjectTask
import com.intellij.task.ProjectTaskRunner
import org.move.cli.moveProjectRoot

// TODO: finish
class AptosBuildTaskRunner: ProjectTaskRunner() {
    override fun canRun(projectTask: ProjectTask): Boolean {
        return when (projectTask) {
            is ModuleBuildTask -> {
                projectTask.module.moveProjectRoot != null
            }
            else -> false
        }
    }
}
