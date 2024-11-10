package org.move.cli.runConfigurations.aptos

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.intellij.lang.annotations.Language

sealed class AptosExitStatus(val message: String) {
    class Result(message: String): AptosExitStatus(message)
    class Error(message: String): AptosExitStatus(message)
    class Malformed(jsonText: String): AptosExitStatus(jsonText)

    companion object {
        @Throws(JacksonException::class)
        fun fromJson(@Language("JSON") json: String): AptosExitStatus {
            val parsedResult = JSON_MAPPER.readValue(json, AptosJsonResult::class.java)
            return when {
                parsedResult.Error != null -> Error(parsedResult.Error)
                parsedResult.Result != null -> Result(parsedResult.Result)
                else -> Malformed(json)
            }
        }
    }
}

@Suppress("PropertyName")
private data class AptosJsonResult(
    val Result: String?,
    val Error: String?
)

private val JSON_MAPPER: ObjectMapper = ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerKotlinModule()
