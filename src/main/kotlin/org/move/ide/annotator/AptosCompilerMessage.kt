package org.move.ide.annotator

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import org.jetbrains.annotations.TestOnly

data class AptosCompilerMessage(
    val text: String,
    val severityLevel: String,
    val spans: List<AptosCompilerSpan>
) {
    val mainSpan: AptosCompilerSpan?
        get() {
            val validSpan = spans.filter { it.isValid() }.firstOrNull { it.isPrimary } ?: return null
            return validSpan
//            return generateSequence(validSpan) { it.expansion?.span }.last()
//                .takeIf { it.isValid() && !it.file_name.startsWith("<") }
        }

    fun toTestString(): String {
        return "$severityLevel: '$text' at ${mainSpan?.toTestString()}"
    }

    companion object {
        @TestOnly
        fun forTest(
            message: String,
            severityLevel: String,
            filename: String,
            location: String,
        ): AptosCompilerMessage {
            val match =
                Regex("""\[\((?<lineStart>\d+), (?<columnStart>\d+)\), \((?<lineEnd>\d+), (?<columnEnd>\d+)\)]""")
                    .find(location) ?: error("invalid string")
            val (lineStart, colStart, lineEnd, colEnd) = match.destructured
            val span = AptosCompilerSpan(
                filename,
                lineStart.toInt(),
                lineEnd.toInt(),
                colStart.toInt(),
                colEnd.toInt(),
                true,
                null
            )
            return AptosCompilerMessage(message, severityLevel, listOf(span))
        }
    }
}

// https://doc.rust-lang.org/nightly/nightly-rustc/syntax/json/struct.DiagnosticSpan.html
data class AptosCompilerSpan(
    val filename: String,
    val lineStart: Int,
    val lineEnd: Int,
    val columnStart: Int,
    val columnEnd: Int,
    val isPrimary: Boolean,
//    val text: List<String>,
    val label: String?,
//    val suggested_replacement: String?,
//    val suggestion_applicability: Applicability?,
//    val expansion: Expansion?
) {
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
