package org.move.cli.externalLinter

import org.move.ide.annotator.externalLinter.AptosCompilerError
import org.move.ide.annotator.externalLinter.CompilerSpan

fun parseHumanCompilerErrors(outputLines: List<String>): List<AptosCompilerError> {
    val rawMessages = splitMessages(outputLines)
    val compilerErrors = rawMessages.mapNotNull(::rawMessageToCompilerMessage)
    return compilerErrors
}

private val ERROR_START_RE = Regex("^error(\\[E\\d+\\])?:\\s+(.+)")

enum class ErrorType(val severityLevel: String) {
    ERROR("error"), WARNING("warning"), WARNING_LINT("warning [lint]");
}

data class RawMessage(val errorType: ErrorType, val lines: MutableList<String>)

private fun splitMessages(outputLines: List<String>): List<RawMessage> {
    val rawMessages = mutableListOf<RawMessage>()
    var message: RawMessage? = null
    for (line in outputLines) {
        if (line.startsWith("{")) {
            break
        }
        val newErrorType = when {
            ERROR_START_RE.find(line) != null -> ErrorType.ERROR
            line.startsWith("warning: [lint]") -> ErrorType.WARNING_LINT
            line.startsWith("warning:") -> ErrorType.WARNING
            else -> null
        }
        if (newErrorType != null) {
            // flush
            message?.let { rawMessages.add(it) }
            message = RawMessage(newErrorType, mutableListOf())
        }
        message?.lines?.add(line)
    }
    // flush
    message?.let { rawMessages.add(it) }
    return rawMessages
}

private fun rawMessageToCompilerMessage(rawMessage: RawMessage): AptosCompilerError? {
    val messageLine = rawMessage.lines.first()
    val message = when (rawMessage.errorType) {
        ErrorType.ERROR -> ERROR_START_RE.find(messageLine)!!.destructured.component2()
        ErrorType.WARNING -> messageLine.substringAfter("warning: ")
        ErrorType.WARNING_LINT -> messageLine.substringAfter("warning: [lint] ")
    }
    val primarySpan = parsePrimarySpan(rawMessage.lines) ?: return null
    return AptosCompilerError(message, rawMessage.errorType.severityLevel, primarySpan)
}

private val FILE_POSITION_RE =
    Regex("""┌─ (?<file>(?:\p{Alpha}:)?[0-9a-z_A-Z\-\\./]+):(?<line>[0-9]+):(?<column>[0-9]+)""")
private val ERROR_UNDERLINE_RE =
    Regex("""^\s*│[^\^]*(\^{2,})""")

private fun parsePrimarySpan(errorLines: List<String>): CompilerSpan? {
    val filePositionMatch =
        errorLines.firstNotNullOfOrNull { FILE_POSITION_RE.find(it) } ?: return null
    val (fpath, lineStart, columnStart) = filePositionMatch.destructured

    val columnSpan = errorLines
        .firstNotNullOfOrNull { ERROR_UNDERLINE_RE.find(it) }
        ?.groupValues?.get(1)
        ?.length ?: 1
    return CompilerSpan(
        fpath,
        lineStart = lineStart.toInt(),
        lineEnd = lineStart.toInt(),
        columnStart = columnStart.toInt(),
        columnEnd = columnStart.toInt() + columnSpan,
        label = null
    )
}