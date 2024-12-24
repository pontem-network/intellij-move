@file:Suppress("PropertyName")

package org.move.ide.annotator.externalLinter

import com.fasterxml.jackson.core.JsonProcessingException
import com.intellij.openapi.util.TextRange
import org.move.cli.runConfigurations.aptos.JSON_MAPPER

fun parseJsonCompilerErrors(outputLines: List<String>): List<JsonAptosCompilerError> {
    return outputLines
        .mapNotNull { JsonAptosCompilerError.fromLine(it) }
}

data class JsonAptosCompilerError(
    val severity: String,
    val code: String?,
    val message: String,
    val labels: List<CodeLabel>,
    val notes: List<String>,
) {
    fun toTestString(): String =
        "$severity: '$message' " +
                "at ${labels.find { it.style == "Primary" }?.range?.toTextRange()}"

    companion object {
        fun fromLine(line: String): JsonAptosCompilerError? {
            if (!line.startsWith("{\"")) return null
            try {
                val compilerMessage =
                    JSON_MAPPER.readValue(line, JsonAptosCompilerError::class.java)
                return compilerMessage
            } catch (_: JsonProcessingException) {
                return null
            }
        }
    }
}

data class CodeLabel(
    val style: String,
    val file_id: String,
    val range: CodeRange,
    val message: String,
)

data class CodeRange(
    val start: Int,
    val end: Int,
) {
    fun toTextRange(): TextRange = TextRange(start, end)
}