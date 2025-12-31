package org.move.cli.runConfigurations.endless

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.intellij.lang.annotations.Language
import org.move.stdext.blankToNull

sealed class EndlessExitStatus(val message: String) {
    class Result(message: String): EndlessExitStatus(message)
    class Error(message: String): EndlessExitStatus(message)
    class Malformed(jsonText: String): EndlessExitStatus(jsonText)

    companion object {
        @Throws(JacksonException::class)
        fun fromJson(@Language("JSON") json: String): EndlessExitStatus {
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
