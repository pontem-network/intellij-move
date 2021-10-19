package org.move.openapiext

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import org.toml.lang.psi.*
import org.toml.lang.psi.ext.elementType
import java.nio.file.Path

fun parseToml(project: Project, path: Path): TomlFile? {
    val file = LocalFileSystem.getInstance().findFileByNioFile(path) ?: return null
    val tomlFileViewProvider =
        PsiManager.getInstance(project).findViewProvider(file) ?: return null
    return TomlFile(tomlFileViewProvider)
}

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

fun TomlFile.getRootKey(key: String): TomlValue? {
    val keyValue = this.children.filterIsInstance<TomlKeyValue>().find { it.key.text == key }
    return keyValue?.value
}
