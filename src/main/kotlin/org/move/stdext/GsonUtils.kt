package org.move.stdext

import com.google.gson.JsonIOException
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import java.io.StringReader

object GsonUtils {
    fun tryParseJsonObject(content: String?, lenient: Boolean = true): JsonObject? =
        try {
            parseJsonObject(content, lenient)
        } catch (ignored: Exception) {
            null
        }

    @Throws(JsonIOException::class, JsonSyntaxException::class, IllegalStateException::class)
    fun parseJsonObject(content: String?, lenient: Boolean = true): JsonObject {
        val jsonReader = JsonReader(StringReader(content ?: ""))
        jsonReader.isLenient = lenient
        return jsonReader.use { JsonParser.parseReader(it).asJsonObject }
    }
}
