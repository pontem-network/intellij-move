package org.move.dove

import com.intellij.execution.configurations.GeneralCommandLine
import org.move.openapiext.GeneralCommandLine
import org.move.openapiext.withWorkDirectory
import java.nio.file.Path

fun command(
    executable: Path,
    workingDirectory: Path,
    command: String,
    additionalArguments: List<String> = emptyList(),
): GeneralCommandLine {
    val arguments = mutableListOf(command)
    arguments.addAll(additionalArguments)
    return GeneralCommandLine(executable)
        .withWorkDirectory(workingDirectory)
        .withParameters(arguments)
        .withEnvironment("TERM", "ansi")
        .withCharset(Charsets.UTF_8)
}

//fun doveCommand() {
//
//}