package org.move.ide.annotator.externalLinter

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.util.PathUtil
import org.jetbrains.annotations.TestOnly
import org.move.stdext.capitalized

data class AptosCompilerError(
    val message: String,
    val severityLevel: String,
    val primarySpan: CompilerSpan,
) {
    fun toTestString(): String {
        return "$severityLevel: '$message' at ${primarySpan.toTestString()}"
    }

    fun toJsonError(file: PsiFile, document: Document): AptosJsonCompilerError? {
        // Some error messages are global, and we *could* show then atop of the editor,
        // but they look rather ugly, so just skip them.
        val errorSpan = this.primarySpan
        val spanFilePath = PathUtil.toSystemIndependentName(errorSpan.filename)
        if (!file.virtualFile.path.endsWith(spanFilePath)) return null

        val codeLabel = this.primarySpan.toCodeLabel(document) ?: return null
        val jsonCompilerError = AptosJsonCompilerError(
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
        ): AptosCompilerError {
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
            return AptosCompilerError(message, severityLevel, span)
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
