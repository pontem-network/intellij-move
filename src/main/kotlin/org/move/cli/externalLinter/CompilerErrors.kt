package org.move.cli.externalLinter

import org.move.ide.annotator.AptosCompilerMessage
import org.move.ide.annotator.AptosCompilerSpan

fun parseCompilerErrors(outputLines: List<String>): List<AptosCompilerMessage> {
    val errorLists = splitErrors(outputLines)
    val messages = errorLists.map(::errorLinesToCompilerMessage)
    return messages
}

private val ERROR_START_RE = Regex("^error(\\[E\\d+\\])?:\\s+(.+)")

private fun splitErrors(outputLines: List<String>): List<List<String>> {
    val errorLists = mutableListOf<List<String>>()
    var thisErrorLines: MutableList<String>? = null
    for (line in outputLines) {
        if (line.startsWith("{")) {
            break
        }
        if (ERROR_START_RE.find(line) != null) {
//        if (line.startsWith("error:")) {
            // flush
            thisErrorLines?.let { errorLists.add(it) }
            thisErrorLines = mutableListOf()
        }
        thisErrorLines?.add(line)
    }
    // flush
    thisErrorLines?.let { errorLists.add(it) }
    return errorLists
}

private fun errorLinesToCompilerMessage(errorLines: List<String>): AptosCompilerMessage {
    val messageLine = errorLines.first()
    val (_, message) = ERROR_START_RE.find(messageLine)!!.destructured
    val spans = splitSpans(errorLines)
    return AptosCompilerMessage(message, "error", spans)
}

private val FILE_POSITION_RE =
    Regex("""┌─ (?<file>(?:\p{Alpha}:)?[0-9a-z_A-Z\-\\./]+):(?<line>[0-9]+):(?<column>[0-9]+)""")
private val ERROR_UNDERLINE_RE =
    Regex("""^\s*│[^\^]*(\^{2,})""")

private fun splitSpans(errorLines: List<String>): List<AptosCompilerSpan> {
    val filePositionMatch =
        errorLines.firstNotNullOfOrNull { FILE_POSITION_RE.find(it) } ?: return emptyList()
    val (fileName, lineStart, columnStart) = filePositionMatch.destructured
    val columnSpan = errorLines
        .firstNotNullOfOrNull { ERROR_UNDERLINE_RE.find(it) }
        ?.groupValues?.get(1)
        ?.length ?: 1
    return listOf(
        AptosCompilerSpan(
            fileName,
            lineStart = lineStart.toInt(),
            lineEnd = lineStart.toInt(),
            columnStart = columnStart.toInt(),
            columnEnd = columnStart.toInt() + columnSpan,
            isPrimary = true,
            label = null
        )
    )
}