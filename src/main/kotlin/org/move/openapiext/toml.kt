package org.move.openapiext

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.toml.lang.psi.*
import org.toml.lang.psi.ext.elementType
import java.nio.file.Path

fun parseToml(project: Project, path: Path): TomlFile? {
    val file = LocalFileSystem.getInstance().findFileByNioFile(path) ?: return null
    return file.toPsiFile(project) as? TomlFile
}

fun parseTomlFromFile(project: Project, moveFile: VirtualFile): TomlFile? {
    return moveFile.toPsiFile(project) as? TomlFile
//    return moveFile.toPsiFile(project) as? TomlFile
}

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

fun TomlFile.getTable(headerText: String): TomlTable? {
    val tables = this.children.filterIsInstance<TomlTable>()
    return tables.find { it.header.key?.text == headerText }
}
