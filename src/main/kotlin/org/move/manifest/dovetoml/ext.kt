package org.move.manifest.dovetoml

import org.toml.lang.psi.*
import org.toml.lang.psi.ext.elementType

fun TomlTable.findValue(key: String): TomlValue? =
    this.entries.findLast { it.key.text == key }?.value

fun TomlInlineTable.findValue(key: String): TomlValue? =
    this.entries.findLast { it.key.text == key }?.value

fun TomlInlineTable.hasKey(key: String): Boolean =
    this.entries.any { it.key.text == key }

fun TomlValue.stringValue(): String? {
    val delimiter = when (firstChild?.elementType) {
        TomlElementTypes.BASIC_STRING -> "\""
        TomlElementTypes.LITERAL_STRING -> "'"
        TomlElementTypes.MULTILINE_BASIC_STRING -> "\"\"\""
        TomlElementTypes.MULTILINE_LITERAL_STRING -> "'''"
        else -> return null
    }
    return text.removeSurrounding(delimiter)
}

fun TomlValue.arrayValue(): List<TomlValue> = (this as? TomlArray)?.elements.orEmpty()
