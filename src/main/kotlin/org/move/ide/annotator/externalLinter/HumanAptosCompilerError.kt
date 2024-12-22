package org.move.ide.annotator.externalLinter

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.util.PathUtil
import org.jetbrains.annotations.TestOnly
import org.move.stdext.capitalized

data class HumanAptosCompilerError(
    val message: String,
    val severityLevel: String,
    val primarySpan: CompilerSpan,
) {
    fun toTestString(): String {
        return "$severityLevel: '$message' at ${primarySpan.toTestString()}"
    }

    fun toJsonError(file: PsiFile, document: Document): JsonAptosCompilerError? {
        val spanFilePath = PathUtil.toSystemIndependentName(this.primarySpan.filename)
        if (!file.virtualFile.path.endsWith(spanFilePath)) return null

        val codeLabel = this.primarySpan.toCodeLabel(document) ?: return null
        val jsonCompilerError = JsonAptosCompilerError(
            severity = this.severityLevel.capitalized(),
            code = null,
            message = this.message,
            labels = listOf(codeLabel),
            notes = listOf()
        )
        return jsonCompilerError
    }

    companion object {
        @TestOnly
        fun forTest(
            message: String,
            severityLevel: String,
            filename: String,
            location: String,
        ): HumanAptosCompilerError {
            val match =
                Regex("""\[\((?<lineStart>\d+), (?<columnStart>\d+)\), \((?<lineEnd>\d+), (?<columnEnd>\d+)\)]""")
                    .find(location) ?: error("invalid string")
            val (lineStart, colStart, lineEnd, colEnd) = match.destructured
            val span = CompilerSpan(
                filename,
                lineStart.toInt(),
                lineEnd.toInt(),
                colStart.toInt(),
                colEnd.toInt(),
                null
            )
            return HumanAptosCompilerError(message, severityLevel, span)
        }
    }
}

// https://doc.rust-lang.org/nightly/nightly-rustc/syntax/json/struct.DiagnosticSpan.html
data class CompilerSpan(
    val filename: String,
    val lineStart: Int,
    val lineEnd: Int,
    val columnStart: Int,
    val columnEnd: Int,
    val label: String?,
) {
    fun toCodeLabel(document: Document): CodeLabel? {
        val textRange = this.toTextRange(document) ?: return null
        return CodeLabel(
            "Primary",
            filename,
            CodeRange(textRange.startOffset, textRange.endOffset),
            ""
        )
    }

    fun toTextRange(document: Document): TextRange? {
        val startOffset = toOffset(document, lineStart, columnStart)
        val endOffset = toOffset(document, lineEnd, columnEnd)
        return if (startOffset != null && endOffset != null && startOffset < endOffset) {
            TextRange(startOffset, endOffset)
        } else {
            null
        }
    }

    fun isValid(): Boolean =
        lineEnd > lineStart || (lineEnd == lineStart && columnEnd >= columnStart)

    fun toTestString(): String {
        return "$filename:[($lineStart, $columnStart), ($lineEnd, $columnEnd)]"
    }

    companion object {
        @Suppress("NAME_SHADOWING")
        fun toOffset(document: Document, line: Int, column: Int): Int? {
            val line = line - 1
            val column = column - 1
            if (line < 0 || line >= document.lineCount) return null
            return (document.getLineStartOffset(line) + column)
                .takeIf { it <= document.textLength }
        }
    }
}


fun parseHumanCompilerErrors(outputLines: List<String>): List<HumanAptosCompilerError> {
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

private fun rawMessageToCompilerMessage(rawMessage: RawMessage): HumanAptosCompilerError? {
    val messageLine = rawMessage.lines.first()
    val message = when (rawMessage.errorType) {
        ErrorType.ERROR -> ERROR_START_RE.find(messageLine)!!.destructured.component2()
        ErrorType.WARNING -> messageLine.substringAfter("warning: ")
        ErrorType.WARNING_LINT -> messageLine.substringAfter("warning: [lint] ")
    }
    // Some error messages are global, and we *could* show then atop of the editor,
    // but they look rather ugly, so just skip them.
    val primarySpan = parsePrimarySpan(rawMessage.lines) ?: return null
    return HumanAptosCompilerError(message, rawMessage.errorType.severityLevel, primarySpan)
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