package org.move.movec.cli

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.move.openapiext.GeneralCommandLine
import org.move.openapiext.execute
import org.move.openapiext.withWorkDirectory
import java.nio.file.Path

fun toGeneralCommandLine(
    executable: Path,
    workingDirectory: Path,
    parameters: List<String>,
): GeneralCommandLine =
    GeneralCommandLine(executable)
        .withWorkDirectory(workingDirectory)
        .withParameters(parameters)
        .withEnvironment("TERM", "ansi")
        .withCharset(Charsets.UTF_8)


class MovecCommandLine(
    private val executable: Path,
    private val command: String,
    private val workingDirectory: Path,
    private val additionalArguments: List<String> = emptyList(),
) {
    fun execute(
        project: Project,
        owner: Disposable = project,
        ignoreExitCode: Boolean = false,
    ): ProcessOutput =
        toGeneralCommandLine(executable, workingDirectory, additionalArguments).execute(owner, ignoreExitCode)
//    constructor(movecProject: MovecProject, command: String, additionalArguments: List<String>) {
//        MovecCommandLine(
//            command,
//            workingDirectory = movecProject.workingDirectory,
//            additionalArguments = additionalArguments)
//    }

//    fun execute(project: Project, owner: Disposable = project, ignoreExitCode: Boolean = false): ProcessOutput {
//        toGeneralCommandLine(movec)
//    }
}
