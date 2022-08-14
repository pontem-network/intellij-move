package org.move.openapiext

import org.move.lang.core.psi.ext.ancestorStrict
import org.toml.lang.psi.*
import org.toml.lang.psi.ext.elementType

fun TomlTable.namedEntries(): List<Pair<String, TomlValue?>> {
    return this.entries.map { Pair(it.key.text, it.value) }
}

fun TomlInlineTable.namedEntries(): List<Pair<String, TomlValue?>> {
    return this.entries.map { it.key.text.trim('"') to it.value }
}

fun TomlTable.findKeyValue(key: String): TomlKeyValue? =
    this.entries.findLast { it.key.text == key }

fun TomlTable.findKey(key: String): TomlKey? =
    this.entries.findLast { it.key.text == key }?.key

fun TomlKeyValue.singleSegmentOrNull(): TomlKeySegment? = this.key.segments.singleOrNull()

fun TomlTable.findValue(key: String): TomlValue? =
    this.entries.findLast { it.key.text == key }?.value

fun TomlInlineTable.findValue(key: String): TomlValue? =
    this.entries.findLast { it.key.text == key }?.value

fun TomlInlineTable.hasKey(key: String): Boolean =
    this.entries.any { it.key.text == key }

val TomlKey.parentTable: TomlTable? get() = (this.parent as? TomlKeyValue)?.parentTable
val TomlKeyValue.parentTable: TomlTable? get() = this.parent as? TomlTable

val TomlElement.moveTomlFile
    get() = this.containingFile?.takeIf { it.name == "Move.toml" } as? TomlFile

val TomlKeySegment.addressesTable: TomlTable?
    get() {
        if (this.moveTomlFile == null) return null
        return this.ancestorStrict<TomlKeyValue>()
            ?.parentTable
            ?.takeIf { it.header.text == "[addresses]" }
    }

val TomlKeyValue.parentInlineTable: TomlInlineTable? get() = this.parent as? TomlInlineTable

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

fun TomlValue.inlineTableValue(): TomlInlineTable? = this as? TomlInlineTable

val TomlValue.keyValue: TomlKeyValue get() = this.parent as TomlKeyValue

fun TomlFile.getRootKey(key: String): TomlValue? {
    val keyValue = this.children.filterIsInstance<TomlKeyValue>().find { it.key.text == key }
    return keyValue?.value
}

fun TomlFile.tables() = this.children.filterIsInstance<TomlTable>()

fun TomlFile.getTable(headerText: String) =
    this.tables().find { it.header.key?.text == headerText }

fun TomlFile.getTablesByFirstSegment(segmentText: String) =
    this.tables()
        .filter { it.header.key?.segments.orEmpty().size == 2 }
        .filter { it.header.key?.segments?.get(0)?.text == segmentText }

typealias TomlElementMap = Map<String, TomlValue?>

fun TomlElement.toMap(): TomlElementMap {
    val namedEntries = when (this) {
        is TomlTable -> this.namedEntries()
        is TomlInlineTable -> this.namedEntries()
        else -> null
    }
    return namedEntries.orEmpty().associate { Pair(it.first, it.second) }
}
