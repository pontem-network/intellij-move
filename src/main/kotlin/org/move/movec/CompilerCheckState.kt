package org.move.movec

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.util.io.systemIndependentPath
import java.nio.file.Path
import java.nio.file.Paths

fun createGeneralCommand(
    executablePath: Path,
    compilerCommand: CompilerCommand
): GeneralCommandLine {
    return GeneralCommandLine(executablePath.systemIndependentPath, compilerCommand.message)
}

class CompilerCheckState(
    environment: ExecutionEnvironment,
    private val runConfiguration: CompilerCheckConfiguration,
    private val compilerCommand: CompilerCommand
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        val compilerCommand = createGeneralCommand(Paths.get("/bin/echo"), compilerCommand)
        val handler = KillableProcessHandler(compilerCommand)
        return handler
    }
}