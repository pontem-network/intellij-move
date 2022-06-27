/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.move.toml.completion

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.intellij.lang.annotations.Language
import org.toml.lang.psi.TomlArrayTable
import org.toml.lang.psi.TomlFileType
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlTable

class MoveTomlSchema private constructor(
    private val tables: List<TomlTableSchema>
) {

    fun topLevelKeys(isArray: Boolean): Collection<String> =
        tables.filter { it.isArray == isArray }.map { it.name }

    fun tableHeaderKeys() = topLevelKeys(false)

    fun keysForTable(tableName: String): Collection<String> =
        tables.find { it.name == tableName }?.keys.orEmpty()

    fun keysForDependency(): Collection<String> =
        listOf("git", "local", "addr_subst", "rev", "branch", "subdir")

    companion object {
        fun parse(project: Project, @Language("TOML") example: String): MoveTomlSchema {
            val toml = PsiFileFactory.getInstance(project)
                .createFileFromText("Move.toml", TomlFileType, example)

            val tables = toml.children
                .filterIsInstance<TomlKeyValueOwner>()
                .mapNotNull { it.schema }

            return MoveTomlSchema(tables)
        }
    }
}

private val TomlKeyValueOwner.schema: TomlTableSchema?
    get() {
        val (name, isArray) = when (this) {
            is TomlTable -> header.key?.segments?.firstOrNull()?.name to false
            is TomlArrayTable -> header.key?.segments?.firstOrNull()?.name to true
            else -> return null
        }
        if (name == null) return null

        val keys = entries.mapNotNull { it.key.text }.filter { it != "foo" }
        return TomlTableSchema(name, isArray, keys)
    }

private class TomlTableSchema(
    val name: String,
    val isArray: Boolean,
    val keys: Collection<String>
)
