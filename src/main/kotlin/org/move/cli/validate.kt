package org.move.cli

data class ValidationError(
    val message: String,
    val line: Int,
    val column: Int,
    val offset: Int
)

data class ValidationOutput(
    val code: Int,
    val error: ValidationError?
)
