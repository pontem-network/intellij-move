package org.move.cli.runConfigurations.aptos

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.intellij.lang.annotations.Language
import org.move.stdext.blankToNull

sealed class AptosExitStatus(val message: String) {
    class Result(message: String): AptosExitStatus(message)
    class Error(message: String): AptosExitStatus(message)
    class Malformed(jsonText: String): AptosExitStatus(jsonText)

    companion object {
        @Throws(JacksonException::class)
        fun fromJson(@Language("JSON") json: String): AptosExitStatus {
            val rootNode = JSON_MAPPER.readTree(json)
            if (rootNode.has("Result")) {
                return Result(nodeToText(rootNode.get("Result")))
            }
            if (rootNode.has("Error")) {
                return Error(nodeToText(rootNode.get("Error")))
            }
            return Malformed(json)
        }

        private fun nodeToText(jsonNode: JsonNode): String {
            // blank for containers
            return jsonNode.asText().blankToNull() ?: jsonNode.toString()
        }
    }
}

val JSON_MAPPER: ObjectMapper = ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .registerKotlinModule()
